package coup.server

import coup.server.dto.LobbyState
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** A lobby from which to start a game. */
class Lobby(
  private val newGame: suspend (players: List<Person>, GameOptions) -> Pair<GameServer, Id>
) {

  private val sessions = MutableStateFlow(mapOf<Id, Session<LobbyState, LobbyCommand>>())

  private val gameOptions = MutableStateFlow(GameOptions.default)
  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<Id?>(null)

  private val state =
    combine(sessions, champion, startingIn, gameOptions) { sessions, champion, startingIn, gameOptions ->
      LobbyState(
        players = sessions.values.map { session ->
          LobbyState.Player(
            session.user.name,
            session.user.color,
            session.userId == champion
          )
        },
        startingIn = startingIn,
        options = gameOptions,
      )
    }

  private val shuttingDown = MutableStateFlow(false)
  val active: Boolean get() = !shuttingDown.value

  suspend fun connect(socket: UserConnection) {
    if (!active) {
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
      launch { manageGameStartTimer(startingIn) }
      launch { forEachSession { processCommands(it.messages) } }
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

  private suspend fun manageGameStartTimer(timer: MutableStateFlow<Int?>) = coroutineScope {
    launch { sessions.collect { timer.value = null } }
    timer.collectLatest { value ->
      when (value) {
        null -> {}
        0 -> {
          startGame()
          delay(5.seconds)
          timer.value = null
        }

        else -> {
          delay(1.seconds)
          timer.value = value - 1
        }
      }
    }
  }

  private suspend fun forEachSession(block: suspend (Session<LobbyState, LobbyCommand>) -> Unit) = coroutineScope {
    val runningJobs = mutableMapOf<Session<LobbyState, LobbyCommand>, Job>()
    this@Lobby.sessions.map { it.values.toSet() }.collect { sessions ->
      runningJobs.minus(sessions).forEach { (_, job) -> job.cancel() }
      runningJobs.keys.retainAll(sessions)
      sessions.minus(runningJobs.keys).forEach { session ->
        runningJobs[session] = launch { block(session) }
      }
    }
  }

  private suspend fun processCommands(commands: Flow<LobbyCommand>) = commands.collect { command ->
    when (command) {
      is LobbyCommand.StartGame -> startingIn.value = 3
      is LobbyCommand.CancelGameStart -> startingIn.value = null
      is LobbyCommand.SetResponseTimer -> {
        startingIn.value = null
        gameOptions.update { it.copy(responseTimer = command.responseTimer) }
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
    val options = gameOptions.value
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