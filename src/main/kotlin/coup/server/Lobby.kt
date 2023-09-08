package coup.server

import coup.game.Game
import coup.server.message.GameStarted
import coup.server.message.LobbyUpdate
import coup.server.message.LobbyUpdate.Lobby
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

class Lobby {
  val id = newId
  private val mutex = Mutex()
  private val players = mutableSetOf<Socket>()
  private var startGameJob: Job? = null

  suspend fun join(player: Socket) {
    mutex.withLock { players.add(player) }
    update()
  }

  suspend fun leave(player: Socket) {
    mutex.withLock { players.remove(player) }
    update()
  }

  fun startGame(scope: CoroutineScope, callback: suspend () -> Unit) =
    scope.launch {
      try {
        repeat(3) { i ->
          update(startingIn = 3 - i)
          delay(1.seconds)
        }
        callback()
      } catch (e: CancellationException) {
        update()
      } finally {
        startGameJob = null
      }
    }

  private suspend fun update(startingIn: Int? = null) = mutex.withLock {
    val lobbyUpdate = LobbyUpdate(Lobby(id, players.map { it.name ?: "<no name>" }, startingIn))
    players.forEach { it.send(lobbyUpdate) }
  }

  suspend fun evacuate(): List<Socket> = mutex.withLock {
    val players = players.toList()
    this.players.clear()
    players
  }
}