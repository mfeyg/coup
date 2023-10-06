package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.message.CancelGameStart
import coup.server.message.GameStarted
import coup.server.message.StartGame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Lobby(
  private val createGame: suspend Lobby.(Iterable<Session<*>>) -> String
) {
  private data class Player(val session: Session<LobbyState>, val connectionCount: Int = 1) {
    val name by session::name
    val id by session::id
    val inc get() = Player(session, connectionCount + 1)
    val dec get() = Player(session, connectionCount - 1)
  }

  private fun newPlayer(connection: SocketConnection) = Player(Session(connection.id, connection.name))

  private operator fun Map<String, Player>.plus(connection: SocketConnection): Map<String, Player> {
    val id by connection::id
    return plus(id to (get(id)?.inc ?: newPlayer(connection)))
  }

  private operator fun Map<String, Player>.minus(connection: SocketConnection): Map<String, Player> {
    val id by connection::id
    val current = get(id) ?: return this
    return if (current.connectionCount == 1) {
      this - id
    } else {
      this + (id to current.dec)
    }
  }

  private val players = MutableStateFlow(mapOf<String, Player>())
  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<String?>(null)
  private val state = combine(players, champion, startingIn) { players, champion, startingIn ->
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
  private var scope: CoroutineScope? = null

  private fun init() {
    scope = scope?.takeIf { it.isActive }
      ?: CoroutineScope(Dispatchers.Default).also { scope ->
        var startGameJob: Job? = null
        scope.launch {
          players.collectLatest { players ->
            if (players.isEmpty()) {
              delay(5.minutes)
              scope.cancel()
            }
            coroutineScope {
              for (player in players.values) {
                launch {
                  state.collect(player.session::setState)
                }
                launch {
                  player.session.messages.collect { message ->
                    when (message) {
                      StartGame -> startGameJob = scope.launch { startGame() }
                      CancelGameStart -> startGameJob?.cancelAndJoin()
                      else -> {}
                    }
                  }
                }
              }
            }
          }
        }
      }
  }

  suspend fun connect(socket: SocketConnection) {
    val (session, _) = players.updateAndGet { it + socket }
      .getValue(socket.id)
    try {
      init()
      session.connect(socket)
    } finally {
      players.update { it - socket }
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
    var players = this.players.value.values.toList()
    val championId = champion.value
    val champion = players.indexOfFirst { it.id == championId }
    if (champion != -1) {
      players = players.subList(champion, players.size) + players.subList(0, champion)
    }
    val game = createGame(players.map { it.session })
    players.forEach { player -> player.session.event(GameStarted(game)) }
  }
}