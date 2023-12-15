package coup.server

import coup.game.*
import coup.game.Game
import coup.game.actions.Action
import coup.game.rules.Ruleset
import coup.game.rules.StandardRules
import coup.server.ConnectionController.SocketConnection
import coup.server.prompt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.hours

class GameServer private constructor(
  private val baseGame: Game,
  private val players: List<Player>,
  private val playerSessions: List<Session<GameState>>,
  private val playerColors: List<String>,
  private val lobby: Lobby,
) {
  private val updates = combine(players.map { it.updates } + baseGame.updates) {}
  private val observers = MutableStateFlow(mapOf<String, Session<GameState>>())
  private val scope = CoroutineScope(Dispatchers.Default)
  private val connectionCount = MutableStateFlow(0)

  init {
    scope.launch {
      updates.onEach {
        playerSessions.forEachIndexed { index, player ->
          player.setState(gameState(index))
        }
      }.launchIn(this)
      launch {
        observers.collectLatest { observers ->
          coroutineScope {
            observers.values.forEach { observer ->
              updates.onEach {
                observer.setState(gameState())
              }.launchIn(this)
            }
          }
        }
      }
      launch {
        connectionCount.collectLatest { connections ->
          if (connections == 0) {
            delay(1.hours)
            scope.cancel()
          }
        }
      }
      baseGame.play()
      baseGame.winner?.let { winner ->
        lobby.setChampion(playerSessions[winner.playerNumber].id)
      }
    }
  }

  companion object {
    suspend operator fun invoke(
      playerSessions: Iterable<Session<*>>,
      lobby: Lobby,
      ruleset: Ruleset = StandardRules()
    ): GameServer {

      val sessions = playerSessions.take(ruleset.maxPlayers).map { it.newSession<GameState>() }
      val players: List<Player> = sessions.mapIndexed { sessionIndex, session ->
        Player(session.name, sessionIndex, ruleset) { player ->
          object : Agent {
            override suspend fun chooseAction(board: Board) =
              ChooseAction(player, session, ruleset).chooseAction(board)

            override suspend fun chooseCardsToReturn(drawnCards: List<Influence>) =
              ExchangeWithDeck(player, session).returnCards(drawnCards)

            override suspend fun chooseReaction(action: Action) =
              RespondToAction(player, session, ruleset).respondToAction(action)

            override suspend fun chooseInfluenceToReveal(claimedInfluence: Influence, challenger: Player) =
              RespondToChallenge(player, session).respondToChallenge(claimedInfluence, challenger)

            override suspend fun chooseWhetherToChallenge(block: Reaction.Block) =
              RespondToBlock(session).challengeBlock(block)

            override suspend fun chooseInfluenceToSurrender() =
              SurrenderInfluence(player, session).surrenderInfluence()
          }
        }
      }
      val baseGame = Game(ruleset, players)
      val playerColors: List<String> = playerSessions.map { idColor(it.id).cssColor }
      return GameServer(baseGame, players, sessions, playerColors, lobby)
    }
  }

  suspend fun connect(connection: SocketConnection) {
    try {
      connectionCount.update { it + 1 }
      session(connection).connect(connection)
    } finally {
      connectionCount.update { it - 1 }
    }
  }

  private fun session(connection: SocketConnection): Session<GameState> =
    playerSession(connection.id) ?: observingSession(connection.id, connection.name)

  private fun playerSession(id: String) = playerSessions.find { it.id == id }

  private fun observingSession(id: String, observerName: String) =
    observers.updateAndGet { observers ->
      if (observers.containsKey(id)) observers else
        observers + (id to Session(id, observerName))
    }.getValue(id)

  private fun gameState(playerNumber: Int? = null) =
    GameState(
      player = playerNumber?.let { players[it] }?.let { player ->
        GameState.Player(
          player.name,
          playerColors[player.playerNumber],
          player.playerNumber,
          player.isk,
          player.heldInfluences,
          player.revealedInfluences,
        )
      },
      players = players.map { opponent ->
        GameState.Opponent(
          opponent.name,
          playerColors[opponent.playerNumber],
          opponent.playerNumber,
          opponent.isk,
          opponent.heldInfluences.size,
          opponent.revealedInfluences,
        )
      },
      currentTurn = baseGame.currentPlayer.playerNumber.takeIf { baseGame.winner == null },
      winner = baseGame.winner?.playerNumber,
    )
}