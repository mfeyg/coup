package coup.server

import coup.game.Board
import coup.game.Game
import coup.game.Player
import coup.server.ConnectionController.SocketConnection
import coup.server.message.Event
import coup.server.message.GameState
import coup.server.prompt.Prompt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Game(players: Iterable<Session<*>>, private val lobby: Lobby) {
  private val players = players.mapIndexed { index, it ->
    Player(it.name, index, object : SocketPlayer() {
      override suspend fun <T> prompt(prompt: Prompt<T>) =
        playerSessions[index].prompt(prompt)
    })
  }
  private val game = Game(Board.setUp(this.players))
  private val playerSessions: List<Session<GameState>> =
    players.mapIndexed { index, it -> Session(it.id, it.name, gameState(index)) }
  private val playerUpdates = combine(this.players.map { it.updates }) { it.toList() }
  private val observers = MutableStateFlow(mapOf<String, Session<GameState>>())
  private val scope by game::scope
  val id get() = game.id

  init {
    scope.launch {
      game.events.onEach { event ->
        playerSessions.forEach { session ->
          session.event(Event(event))
        }
      }.launchIn(this)
      combine(playerUpdates, game.turns) { _, _ ->
        playerSessions.forEachIndexed { index, player ->
          player.setState(gameState(index))
        }
      }.launchIn(this)
      launch {
        observers.collectLatest { observers ->
          coroutineScope {
            observers.values.forEach { observer ->
              combine(playerUpdates, game.turns) { _, _ ->
                observer.setState(gameState())
              }.launchIn(this)
              game.events.onEach { event -> observer.event(Event(event)) }
            }
          }
        }
      }
      game.start()
    }
  }

  suspend fun connect(connection: SocketConnection) {
    playerSessions.find { session -> session.id == connection.id }?.let { session ->
      session.connect(connection)
      return
    }
    val observers = observers.updateAndGet { observers ->
      if (observers.containsKey(connection.id)) observers else
        observers + (connection.id to Session(connection.id, connection.name, gameState()))
    }
    observers.getValue(connection.id).connect(connection)
  }

  private fun gameState(forPlayer: Int? = null) =
    GameState(
      forPlayer?.let { players[it] }?.let { player ->
        GameState.Player(
          player.name,
          player.playerNumber,
          player.isk,
          player.heldInfluences,
          player.revealedInfluences,
        )
      },
      players.filter { it.playerNumber != forPlayer }.map { opponent ->
        GameState.Opponent(
          opponent.name,
          opponent.playerNumber,
          opponent.isk,
          opponent.heldInfluences.size,
          opponent.revealedInfluences,
        )
      },
      game.currentPlayer.playerNumber,
    )
}