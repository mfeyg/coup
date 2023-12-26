package coup.server

import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Lobby(
  private val createGame: suspend Lobby.(List<Session<*, *>>) -> String
) {

  private sealed interface LobbyCommand {
    data object StartGame : LobbyCommand
    data object CancelGameStart : LobbyCommand
    data class SetResponseTimer(val responseTimer: Int?) : LobbyCommand {
      companion object {
        val PATTERN = Regex("SetResponseTimer:(null|\\d+)")
        fun parse(input: String): SetResponseTimer {
          val (value) = PATTERN.matchEntire(input)?.destructured
            ?: throw IllegalArgumentException("Unexpected input: $input")
          return SetResponseTimer(
            when (value) {
              "null" -> null
              else -> value.toInt()
            }
          )
        }
      }
    }

    companion object {
      fun valueOf(command: String) = when {
        command == "StartGame" -> StartGame
        command == "CancelGameStart" -> CancelGameStart
        command matches SetResponseTimer.PATTERN -> SetResponseTimer.parse(command)
        else -> throw IllegalArgumentException("Unknown command $command")
      }
    }
  }

  private val sessions = MutableStateFlow(mapOf<String, Session<LobbyState, LobbyCommand>>())

  private val options = MutableStateFlow(Options())
  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<String?>(null)
  private val state = combine(sessions, champion, startingIn, options) { players, champion, startingIn, options ->
    LobbyState(
      players = players.values.map { player ->
        LobbyState.Player(
          player.name,
          idColor(player.id).cssColor,
          player.id == champion
        )
      },
      startingIn = startingIn,
      options = options,
    )
  }
  val isActive: Boolean get() = !shuttingDown.value
  private val shuttingDown = MutableStateFlow(false)

  private fun CoroutineScope.eachSession(block: suspend (Session<LobbyState, LobbyCommand>) -> Unit) = launch {
    val runningJobs = mutableMapOf<Session<LobbyState, LobbyCommand>, Job>()
    this@Lobby.sessions.map { it.values.toSet() }.collect { sessions ->
      runningJobs.minus(sessions).forEach { (_, job) -> job.cancel() }
      runningJobs.keys.retainAll(sessions)
      sessions.minus(runningJobs.keys).forEach { session ->
        runningJobs[session] = launch { block(session) }
      }
    }
  }

  init {
    with(CoroutineScope(Dispatchers.Default)) {
      val scope = this
      eachSession { session ->
        session.messages.collect { command ->
          when (command) {
            is LobbyCommand.StartGame -> startingIn.value = 3
            is LobbyCommand.CancelGameStart -> startingIn.value = null
            is LobbyCommand.SetResponseTimer -> {
              startingIn.value = null
              options.update { it.copy(responseTimer = command.responseTimer) }
            }
          }
        }
      }
      launch {
        sessions.collectLatest { sessions ->
          startingIn.value = null
          if (sessions.isEmpty()) {
            delay(5.minutes)
            shutdown(scope)
          }
        }
      }
      launch {
        startingIn.collectLatest startingIn@{ value ->
          if (value == null) return@startingIn
          when (value) {
            0 -> {
              startGame()
              delay(5.seconds)
              startingIn.value = null
            }

            else -> {
              delay(1.seconds)
              startingIn.value = value - 1
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
        state,
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