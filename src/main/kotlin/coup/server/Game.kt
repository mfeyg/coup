package coup.server

import coup.game.Board
import coup.game.Game
import coup.game.Player
import coup.server.message.Event
import coup.server.message.GameStarted
import coup.server.message.GameUpdate
import coup.server.message.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Game(private val sockets: List<Socket>) {
  private val players: List<Player> by lazy {
    sockets.mapIndexed { index, socket ->
      Player(socket.name ?: throw ServerError("Player name required"),
        index, SocketPlayer(socket) { players[index] })
    }
  }
  private val base = Game(Board.setUp(this.players))
  private val scope = CoroutineScope(Dispatchers.Default)
  private val mutex = Mutex()
  private var job: Job? = null
  val id get() = base.id

  private val Player.socket get() = sockets[playerNumber]
  private suspend fun Player.send(message: Message) = socket.send(message)

  fun start() {
    job = scope.launch {
      send(GameStarted(id))
      base.events.onEach { event -> send(Event(event)) }.launchIn(this)
      base.turns.onEach { updateGameState() }.launchIn(this)
      players.forEach { player -> player.updates.onEach { updateGameState() }.launchIn(this) }
      base.start()
    }
  }

  private suspend fun send(message: Message) = mutex.withLock {
    players.forEach { player -> player.send(message) }
  }

  private suspend fun updateGameState() = mutex.withLock {
    players.forEach { player ->
      player.socket.state.value = GameUpdate(
        GameUpdate.GameModel(
          player.name,
          player.playerNumber,
          player.isk,
          player == base.currentPlayer,
          player.heldInfluences,
          player.revealedInfluences,
          (players - player).filter { it.isActive }.map { opponent ->
            GameUpdate.GameModel.Opponent(
              opponent.name,
              opponent.playerNumber,
              opponent.isk,
              base.currentPlayer == opponent,
              opponent.heldInfluences.size,
              opponent.revealedInfluences,
            )
          },
          base.currentPlayer.playerNumber,
        )
      )
    }
  }
}