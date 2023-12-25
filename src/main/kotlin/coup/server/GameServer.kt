package coup.server

import coup.game.*
import coup.game.Game
import coup.game.actions.Action
import coup.game.rules.Ruleset
import coup.server.ConnectionController.SocketConnection
import coup.server.prompt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.hours

class GameServer private constructor(
  private val baseGame: Game,
  private val players: List<Player>,
  private val playerColor: (Player) -> String,
  private val connectPlayer: suspend (SocketConnection) -> Unit?,
  private val setWinner: (Player) -> Unit,
) {
  private val observers = MutableStateFlow(mapOf<String, Session<GameState, Nothing>>())
  private val scope = CoroutineScope(Dispatchers.Default)
  private val connectionCount = MutableStateFlow(0)

  private val _onShutDown = MutableStateFlow(listOf<() -> Unit>())
  fun onShutDown(block: () -> Unit) = _onShutDown.update { it + block }

  private fun state(playerNumber: Int? = null) = combine(players.map { it.updates } + baseGame.updates) {
    GameState(
      player = playerNumber?.let { players[it] }?.let { player ->
        GameState.Player(
          player.name,
          playerColor(player),
          player.playerNumber,
          player.isk,
          player.heldInfluences,
          player.revealedInfluences,
        )
      },
      players = players.map { opponent ->
        GameState.Opponent(
          opponent.name,
          playerColor(opponent),
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

  fun start() {
    scope.launch {
      connectionCount.collectLatest { connections ->
        if (connections == 0) {
          delay(1.hours)
          _onShutDown.value.forEach { it() }
          scope.cancel()
        }
      }
    }
    scope.launch {
      baseGame.play()
      baseGame.winner?.let { setWinner(it) }
    }
  }

  companion object {
    suspend operator fun invoke(
      playerIds: List<String>,
      playerNames: List<String>,
      lobby: Lobby,
      ruleset: Ruleset,
    ): GameServer {

      val numPlayers = playerIds.size
      val sessions = object {
        lateinit var value: List<Session<GameState, Nothing>>
      }

      fun playerSession(player: Player) = sessions.value[player.playerNumber]
      val playerNumberById = buildMap {
        playerIds.forEachIndexed { index, id ->
          put(id, index)
        }
      }

      fun session(id: String) = playerNumberById[id]?.let { sessions.value[it] }

      class PlayerAgent(private val player: Player) : Agent {
        override suspend fun chooseAction(board: Board) =
          ChooseAction(player, playerSession(player), ruleset).chooseAction(board)

        override suspend fun chooseCardsToReturn(drawnCards: List<Influence>) =
          ExchangeWithDeck(player, playerSession(player)).returnCards(drawnCards)

        override suspend fun chooseReaction(action: Action) =
          RespondToAction(player, playerSession(player), ruleset).respondToAction(action)

        override suspend fun chooseInfluenceToReveal(claimedInfluence: Influence, challenger: Player) =
          RespondToChallenge(player, playerSession(player)).respondToChallenge(claimedInfluence, challenger)

        override suspend fun chooseWhetherToChallenge(block: Reaction.Block) =
          RespondToBlock(playerSession(player)).challengeBlock(block)

        override suspend fun chooseInfluenceToSurrender() =
          SurrenderInfluence(player, playerSession(player)).surrenderInfluence()
      }

      val players: List<Player> = List(numPlayers) { playerNumber ->
        Player(playerNames[playerNumber], playerNumber, ruleset, ::PlayerAgent)
      }
      val playerColors: List<String> = playerIds.map { idColor(it).cssColor }

      val gameServer = GameServer(
        Game(ruleset, players),
        players,
        { playerColors[it.playerNumber] },
        { session(it.id)?.connect(it) },
        { lobby.setChampion(playerIds[it.playerNumber]) })

      sessions.value = List(numPlayers) { playerNumber ->
        Session(
          playerIds[playerNumber],
          playerNames[playerNumber],
          gameServer.state(playerNumber)
        )
      }
      return gameServer
    }
  }

  suspend fun connect(connection: SocketConnection) {
    try {
      connectionCount.update { it + 1 }
      connectPlayer(connection)
        ?: observingSession(connection.id, connection.name).connect(connection)
    } finally {
      connectionCount.update { it - 1 }
    }
  }

  private fun observingSession(id: String, observerName: String) =
    observers.updateAndGet { observers ->
      if (observers.containsKey(id)) observers else
        observers + (id to Session(id, observerName, state()))
    }.getValue(id)
}