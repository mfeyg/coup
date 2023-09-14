package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.message.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

typealias LobbySession = Session<LobbyState>

class Lobby(private val newGame: suspend Lobby.(Iterable<Session<*>>) -> Game, private val newLobby: suspend () -> Lobby) {
  val id = newId

  private val players = MutableStateFlow(mapOf<String, LobbySession>())
  private val startingIn = MutableStateFlow<Int?>(null)
  private val state = combine(players, startingIn) { players, startingIn ->
    LobbyState(id, players.values.map { it.name }, startingIn)
  }

  private val mutex = Mutex()

  private val listener = DynamicTask {
    var startGameJob: Job? = null
    launch {
      players.collectLatest { currentPlayers ->
        coroutineScope {
          for (player in currentPlayers.values) {
            state.onEach { state -> player.setState(state) }.launchIn(this)
            player.messages.onEach { message ->
              when (message) {
                StartGame -> startGameJob = this@DynamicTask.launch { startGame() }
                CancelGameStart -> startGameJob?.cancelAndJoin()
                NewLobby -> player.event(LobbyCreated(newLobby().id))
                else -> {}
              }
            }.launchIn(this)
            player.active.onEach { playerActive ->
              mutex.withLock {
                if (!playerActive) players.update { it - player.id }
              }
            }.launchIn(this)
          }
        }
      }
    }
  }

  suspend fun connect(socket: SocketConnection) {
    val session = sessionFor(socket)
    listener.runWhile {
      session.connect(socket)
    }
  }

  private suspend fun sessionFor(socket: SocketConnection) = sessionFor(socket.id, socket.name)

  private suspend fun sessionFor(id: String, name: String) = mutex.withLock {
    val session = players.value[id] ?: newSession(id, name)
    players.update { it + (session.id to session) }
    session
  }

  private suspend fun newSession(id: String, name: String) = Session(id, name, state.first())

  private suspend fun startGame() {
    try {
      repeat(3) { i ->
        startingIn.value = 3 - i
        delay(1.seconds)
      }
      newGame()
    } finally {
      startingIn.value = null
    }
  }

  private suspend fun newGame() {
    val players = this.players.value.values
    val game = newGame(players)
    players.forEach { player -> player.event(GameStarted(game.id)) }
  }
}