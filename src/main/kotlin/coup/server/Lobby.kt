package coup.server

import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Lobby(
  private val createGame: suspend Lobby.(Iterable<Session<*, *>>) -> String
) {

  private enum class LobbyCommand { StartGame, CancelGameStart }

  private val sessions = MutableStateFlow(mapOf<String, Session<LobbyState, LobbyCommand>>())

  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<String?>(null)
  private val state = combine(sessions, champion, startingIn) { players, champion, startingIn ->
    LobbyState(
      players.values.map { player ->
        LobbyState.Player(
          player.name,
          idColor(player.id).cssColor,
          player.id == champion
        )
      },
      startingIn
    )
  }
  val isActive: Boolean get() = !shuttingDown.value
  private val shuttingDown = MutableStateFlow(false)

  init {
    with(CoroutineScope(Dispatchers.Default)) {
      launch {
        sessions.collectLatest { sessions ->
          if (sessions.isEmpty()) {
            delay(5.minutes)
            shutdown(this)
          }
          coroutineScope {
            for (player in sessions.values) {
              launch {
                state.collect(player::setState)
              }
              launch {
                player.messages.collect { message ->
                  when (message) {
                    LobbyCommand.StartGame -> startingIn.value = 3
                    LobbyCommand.CancelGameStart -> startingIn.value = null
                  }
                }
              }
            }
          }
        }
      }
      launch {
        startingIn.collectLatest startingIn@{ value ->
          if (value == null) return@startingIn
          when (value) {
            0 -> startGame()
            else -> {
              delay(1.seconds)
              startingIn.update { value - 1 }
            }
          }
        }
      }
    }
  }

  private val _onShutDown = MutableStateFlow(listOf<() -> Unit>())
  fun onShutDown(block: () -> Unit) = _onShutDown.update { it + block }

  private fun shutdown(scope: CoroutineScope) {
    shuttingDown.value = true
    for (session in sessions.value.values) {
      session.disconnect()
    }
    _onShutDown.value.forEach { it() }
    scope.cancel()
  }

  suspend fun connect(socket: SocketConnection) {
    if (!isActive) {
      throw IllegalStateException("Lobby is closed")
    }
    val session = sessions.updateAndGet { sessions ->
      if (!sessions.containsKey(socket.id)) sessions + (socket.id to Session(
        socket.id,
        socket.name,
        LobbyCommand::valueOf,
      )) else sessions
    }.getValue(socket.id)
    try {
      session.connect(socket)
    } finally {
      sessions.update { sessions -> if (session.connectionCount == 0) sessions - session.id else sessions }
    }
  }

  fun setChampion(id: String) {
    champion.value = id
  }

  private suspend fun startGame() {
    var players = this.sessions.value.values.toList()
    val championId = champion.value
    val champion = players.indexOfFirst { it.id == championId }
    if (champion != -1) {
      players = players.subList(champion, players.size) + players.subList(0, champion)
    }
    val game = createGame(players)
    players.forEach { player -> player.event("GameStarted:$game") }
  }
}