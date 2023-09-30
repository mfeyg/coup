package coup.server

import coup.game.Board
import coup.game.Game
import coup.game.GameEvent
import coup.game.Player
import coup.server.ConnectionController.SocketConnection
import coup.server.prompt.Prompt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.minutes

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
      baseGame.events.onEach { event ->
        this@Game.playerSessions.forEach { session ->
          session.event(Event(event))
        }
      }.launchIn(this)
      combine(playerUpdates, baseGame.turns, baseGame.winner) { _, _, _ ->
        this@Game.playerSessions.forEachIndexed { index, player ->
          player.setState(gameState(index))
        }
      }.launchIn(this)
      launch {
        observers.collectLatest { observers ->
          coroutineScope {
            observers.values.forEach { observer ->
              combine(playerUpdates, baseGame.turns, baseGame.winner) { _, _, _ ->
                observer.setState(gameState())
              }.launchIn(this)
              baseGame.events.onEach { event -> observer.event(Event(event)) }
            }
          }
        }
      }
      launch {
        connectionCount.collectLatest { connections ->
          if (connections == 0) {
            delay(5.minutes)
            scope.cancel()
          }
        }
      }
      baseGame.events.filterIsInstance<GameEvent.GameOver>()
        .onEach { (winner) -> lobby.setChampion(playerSessions[winner].id) }
        .launchIn(this)
      baseGame.start()
    }
  }

  companion object {
    suspend fun new(playerSessions: Iterable<Session<*>>, lobby: Lobby): coup.server.Game {

      val sessions = playerSessions.map { it.newSession<GameState>() }
      val players: List<Player> = playerSessions.mapIndexed { index, it ->
        Player(it.name, index, object : SocketPlayer() {
          override suspend fun <T> prompt(prompt: Prompt<T>) = sessions[index].prompt(prompt)
        })
      }
      val baseGame = Game(Board.setUp(players))
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
      baseGame.currentPlayer.playerNumber.takeIf { baseGame.winner.value == null },
      baseGame.winner.value?.playerNumber,
    )
}