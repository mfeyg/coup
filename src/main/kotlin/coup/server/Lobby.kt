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
  private val players = MutableStateFlow(mapOf<String, Pair<Session<LobbyState>, Int>>())
  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<String?>(null)
  private val state = combine(players, champion, startingIn) { players, champion, startingIn ->
    LobbyState(
      players.values.map { (player, _) ->
        LobbyState.Player(
          player.name,
          idColor(player.id).cssColor,
          player.id == champion
        )
      },
      startingIn
    )
  }
  private val scope = CoroutineScope(Dispatchers.Default)

  init {
    var startGameJob: Job? = null
    scope.launch {
      players.collectLatest { players ->
        if (players.isEmpty()) {
          delay(5.minutes)
          scope.cancel()
        }
        coroutineScope {
          for ((player, _) in players.values) {
            launch {
              state.collect(player::setState)
            }
            launch {
              player.messages.collect { message ->
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

  suspend fun connect(socket: SocketConnection) {
    fun newSession() = Session<LobbyState>(socket.id, socket.name) to 0
    val (session, _) = players.updateAndGet { players ->
      players.plus(socket.id to run {
        val (session, count) = players[socket.id]?.takeIf { (_, count) -> count > 0 } ?: newSession()
        session to (count + 1)
      })
    }.getValue(socket.id)
    try {
      session.connect(socket)
    } finally {
      players.update { players ->
        val (_, count) = players[session.id]!!
        if (count > 1) {
          players + (session.id to (session to (count - 1)))
        } else {
          players - session.id
        }
      }
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
    val champion = players.indexOfFirst { (it, _) -> it.id == championId }
    if (champion != -1) {
      players = players.subList(champion, players.size) + players.subList(0, champion)
    }
    val game = createGame(players.map { it.first })
    players.forEach { (player, _) -> player.event(GameStarted(game)) }
  }
}