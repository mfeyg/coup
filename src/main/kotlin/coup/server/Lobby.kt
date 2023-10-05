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
  private val players = MutableStateFlow(mapOf<String, Session<LobbyState>>())
  private val startingIn = MutableStateFlow<Int?>(null)
  private val champion = MutableStateFlow<String?>(null)
  private val state = combine(players, champion, startingIn) { players, champion, startingIn ->
    LobbyState(
      players.values.map { LobbyState.Player(it.name, idColor(it.id).cssColor, it.id == champion) },
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
          for (player in players.values) {
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
            launch {
              player.active.collect { active ->
                if (!active) this@Lobby.players.update { it - player.id }
              }
            }
          }
        }
      }
    }
  }

  suspend fun connect(socket: SocketConnection) {
    players.updateAndGet { players ->
      if (!players.containsKey(socket.id)) {
        players + (socket.id to Session(socket.id, socket.name))
      } else players
    }.getValue(socket.id).connect(socket)
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
    val game = createGame(players)
    players.forEach { player -> player.event(GameStarted(game)) }
  }
}