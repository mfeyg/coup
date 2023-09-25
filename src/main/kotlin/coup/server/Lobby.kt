package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.message.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

typealias LobbySession = Session<LobbyState>

class Lobby(
  private val createGame: suspend Lobby.(Iterable<Session<*>>) -> String
) {
  private val players = MutableStateFlow(mapOf<String, LobbySession>())
  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<String?>(null)
  private val state = combine(players, champion, startingIn) { players, champion, startingIn ->
    LobbyState(
      players.values.map { LobbyState.Player(it.name, idColor(it.id).cssColor, it.id == champion) },
      startingIn
    )
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

  fun setChampion(id: String) {
    champion.value = id
  }

  private suspend fun sessionFor(socket: SocketConnection) = mutex.withLock {
    val session = players.value[socket.id] ?: Session(socket.id, socket.name)
    players.update { it + (session.id to session) }
    session
  }

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
    val game = createGame(players)
    players.forEach { player -> player.event(GameStarted(game)) }
  }
}