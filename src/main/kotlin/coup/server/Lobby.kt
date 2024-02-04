package coup.server

import coup.server.dto.LobbyState
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Lobby(
  private val newGame: suspend (players: List<Person>, GameOptions) -> Pair<GameServer, Id>
) {

  private val sessions = MutableStateFlow(mapOf<Id, Session<LobbyState, LobbyCommand>>())

  private val options = MutableStateFlow(GameOptions.default)
  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<Id?>(null)
  private val state = combine(sessions, champion, startingIn, options) { players, champion, startingIn, options ->
    LobbyState(
      players = players.values.map { player ->
        LobbyState.Player(
          player.user.name,
          player.user.color,
          player.userId == champion
        )
      },
      startingIn = startingIn,
      options = options,
    )
  }
  val isActive: Boolean get() = !shuttingDown.value
  private val shuttingDown = MutableStateFlow(false)

  suspend fun connect(socket: UserConnection) {
    if (!isActive) {
      socket.send("LobbyNotFound")
      return
    }
    val session = sessions.updateAndGet { sessions ->
      if (!sessions.containsKey(socket.user.id)) sessions + (socket.user.id to Session(
        socket.user,
        state,
        LobbyCommand::valueOf,
      )) else sessions
    }.getValue(socket.user.id)
    try {
      session.connect(socket)
    } finally {
      sessions.update { sessions -> if (session.connectionCount == 0) sessions - session.userId else sessions }
    }
  }

  fun start() {
    with(CoroutineScope(Dispatchers.Default)) {
      shutDownWhenEmptyFor(5.minutes)
      launch { respondToCommands() }
      launch { gameStartTimer() }
    }
  }

  private fun CoroutineScope.shutDownWhenEmptyFor(delay: Duration) = launch {
    sessions.collectLatest { value ->
      if (value.isEmpty()) {
        delay(delay)
        shuttingDown.value = true
        for (session in sessions.value.values) {
          session.disconnect()
        }
        _onShutDown.value.forEach { it() }
        this@shutDownWhenEmptyFor.cancel()
      }
    }
  }

  private suspend fun eachSession(block: suspend (Session<LobbyState, LobbyCommand>) -> Unit) = coroutineScope {
    val runningJobs = mutableMapOf<Session<LobbyState, LobbyCommand>, Job>()
    this@Lobby.sessions.map { it.values.toSet() }.collect { sessions ->
      runningJobs.minus(sessions).forEach { (_, job) -> job.cancel() }
      runningJobs.keys.retainAll(sessions)
      sessions.minus(runningJobs.keys).forEach { session ->
        runningJobs[session] = launch { block(session) }
      }
    }
  }

  private suspend fun respondToCommands() {
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
  }

  private suspend fun gameStartTimer() = coroutineScope {
    launch { sessions.collect { startingIn.value = null } }
    startingIn.collectLatest { value ->
      when (value) {
        null -> {}
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
  private val _onShutDown = MutableStateFlow(listOf<() -> Unit>())

  fun onShutDown(block: () -> Unit) = _onShutDown.update { it + block }

  private fun setChampion(champion: Person) {
    this.champion.value = champion.id
  }

  private suspend fun startGame() {
    var players = this.sessions.value.values.toList()
    val options = options.value
    val championId = champion.value
    val champion = players.indexOfFirst { it.userId == championId }
    if (champion != -1) {
      players = players.subList(champion, players.size) + players.subList(0, champion)
    }
    val (game, gameId) = newGame(players.map { it.user }, options)
    game.onComplete {
      it.winner?.let { winner ->
        setChampion(players[winner.number].user)
      }
    }
    players.forEach { player -> player.event("GameStarted:${gameId.value}") }
  }
}