package coup.server

import coup.game.*
import coup.game.Game
import coup.game.rules.Ruleset
import coup.game.rules.StandardRules
import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.hours

class Game private constructor(
  private val baseGame: Game,
  private val players: List<Player>,
  private val playerSessions: List<Session<GameState>>,
  private val playerColors: List<String>,
  private val lobby: Lobby,
) {
  private val playerUpdates = combine(this.players.map { it.updates }) { it.toList() }
  private val observers = MutableStateFlow(mapOf<String, Session<GameState>>())
  private val scope = CoroutineScope(Dispatchers.Default)
  private val connectionCount = MutableStateFlow(0)

  init {
    scope.launch {
      combine(playerUpdates, baseGame.currentPlayer) { _, _ ->
        this@Game.playerSessions.forEachIndexed { index, player ->
          player.setState(gameState(index))
        }
      }.launchIn(this)
      launch {
        observers.collectLatest { observers ->
          coroutineScope {
            observers.values.forEach { observer ->
              combine(playerUpdates, baseGame.currentPlayer) { _, _ ->
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
      baseGame.start()
      baseGame.winner?.let { winner -> lobby.setChampion(playerSessions[winner.playerNumber].id) }
    }
  }

  companion object {
    suspend fun new(
      playerSessions: Iterable<Session<*>>,
      lobby: Lobby,
      ruleset: Ruleset = StandardRules()
    ): coup.server.Game {

      val sessions = playerSessions.map { it.newSession<GameState>() }
      val players: List<Player> = playerSessions.mapIndexed { index, it ->
        Player(it.name, index, SocketPlayer(ruleset, sessions[index]), ruleset)
      }
      val baseGame = Game(ruleset, players)
      val playerColors: List<String> = playerSessions.map { idColor(it.id).cssColor }
      return Game(baseGame, players, sessions, playerColors, lobby)
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
      playerNumber?.let { players[it] }?.let { player ->
        GameState.Player(
          player.name,
          playerColors[player.playerNumber],
          player.playerNumber,
          player.isk,
          player.heldInfluences,
          player.revealedInfluences,
        )
      },
      players.map { opponent ->
        GameState.Opponent(
          opponent.name,
          playerColors[opponent.playerNumber],
          opponent.playerNumber,
          opponent.isk,
          opponent.heldInfluences.size,
          opponent.revealedInfluences,
        )
      },
      baseGame.currentPlayer.value.playerNumber.takeIf { baseGame.winner == null },
      baseGame.winner?.playerNumber,
    )
}