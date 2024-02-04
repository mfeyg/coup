package coup.server

import coup.game.Game
import coup.game.Player
import coup.server.dto.GameState
import coup.server.dto.PlayerData.Companion.dto
import coup.server.dto.CurrentPlayerData.Companion.asCurrentPlayer
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

  private fun state(player: () -> Player? = { null }) = version.map {
    GameState(
      player = player()?.asCurrentPlayer(),
      players = game.players.map { it.dto() },
      currentTurn = game.activePlayer.number.takeIf { game.winner == null },
      winner = game.winner?.number,
    )
  }

  fun session(person: Person) = sessions.updateAndGet sessions@{ value ->
    if (value.containsKey(person.id)) return@sessions value
    val playerIndex = players.indexOfFirst { it.id == person.id }.takeIf { it >= 0 }
    val gameState = state { playerIndex?.let { game.players[it] } }
    return@sessions value + (person.id to Session(person, gameState))
  }.getValue(person.id)

  suspend fun connect(connection: UserConnection) {
    try {
      connectionCount.update { it + 1 }
      session(connection.user).connect(connection)
    } finally {
      connectionCount.update { it - 1 }
    }
  }

  fun start() {
    with(CoroutineScope(Dispatchers.Default)) {
      val scope = this
      launch {
        connectionCount.collectLatest { connections ->
          if (connections == 0) {
            delay(1.hours)
            _onShutDown.value.forEach { it() }
            scope.cancel()
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