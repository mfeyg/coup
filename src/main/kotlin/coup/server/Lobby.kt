package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.message.CancelGameStart
import coup.server.message.StartGame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Lobby(
  private val createGame: suspend Lobby.(Iterable<Session<*>>) -> String
) {

  private val sessions = MutableStateFlow(mapOf<String, Session<LobbyState>>())

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
    CoroutineScope(Dispatchers.Default).also { scope ->
      var startGameJob: Job? = null
      scope.launch {
        sessions.collectLatest { sessions ->
          if (sessions.isEmpty()) {
            delay(5.minutes)
            shutdown(scope)
          }
          coroutineScope {
            for (player in sessions.values) {
              launch {
                state.collect(player::setState)
              }
              launch {
                player.messages.collect { message ->
                  when (message) {
                    StartGame -> startGameJob = scope.launch { startGame() }
                    CancelGameStart -> startGameJob?.cancelAndJoin()
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private fun shutdown(scope: CoroutineScope) {
    shuttingDown.value = true
    for (session in sessions.value.values) {
      session.disconnect()
    }
    scope.cancel()
  }

  suspend fun connect(socket: SocketConnection) {
    if (!isActive) {
      throw IllegalStateException("Lobby is closed")
    }
    val session = sessions.updateAndGet { sessions ->
      if (!sessions.containsKey(socket.id)) sessions + (socket.id to Session(socket.id, socket.name)) else sessions
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