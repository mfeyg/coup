package coup.server

import coup.game.Game
import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.hours

class GameServer(
  private val players: List<Person>,
  game: (GameServer) -> Game,
) {
  private val sessions = MutableStateFlow(mapOf<Id, Session<GameState, Nothing>>())
  private val version = MutableStateFlow(0)
  private val connectionCount = MutableStateFlow(0)

  private val game = game(this)

  private val _onComplete = MutableStateFlow(listOf<(Game) -> Unit>())
  fun onComplete(block: (Game) -> Unit) = _onComplete.update { it + block }

  private val _onShutDown = MutableStateFlow(listOf<() -> Unit>())
  fun onShutDown(block: () -> Unit) = _onShutDown.update { it + block }

  private fun state(playerIndex: Int? = null) = version.map {
    GameState(
      player = playerIndex?.let { i -> GameState.Player(players[i], game.players[i]) },
      players = players.indices.map { i -> GameState.Opponent(players[i], game.players[i]) },
      currentTurn = game.currentPlayer.number.takeIf { game.winner == null },
      winner = game.winner?.number,
    )
  }

  private operator fun Map<Id, Session<GameState, Nothing>>.plus(person: Person): Map<Id, Session<GameState, Nothing>> {
    if (containsKey(person.id)) return this
    val playerIndex = players.indexOfFirst { it.id == person.id }.takeIf { it >= 0 }
    val gameState = state(playerIndex)
    return this + (person.id to Session(person, gameState))
  }

  fun session(person: Person) = sessions.updateAndGet { it + person }.getValue(person.id)

  suspend fun connect(connection: SocketConnection) {
    try {
      connectionCount.update { it + 1 }
      session(connection.user).connect(connection)
    } finally {
      connectionCount.update { it - 1 }
    }
  }

  fun start() {
    with(CoroutineScope(Dispatchers.Default)) {
      val outer = this
      launch {
        connectionCount.collectLatest { connections ->
          if (connections == 0) {
            delay(1.hours)
            _onShutDown.value.forEach { it() }
            outer.cancel()
          }
        }
      }
      launch {
        launch { game.updates.collect { version.update { it + 1 } } }
        game.play()
        _onComplete.value.forEach { it(game) }
      }
    }
  }

  companion object {
    operator fun invoke(build: GameBuilder.() -> Unit) = GameBuilder(build)
  }
}