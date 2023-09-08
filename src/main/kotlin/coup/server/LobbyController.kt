package coup.server

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class LobbyController {
  val defaultLobby = Lobby()
  private val lobbies = Collections.newSetFromMap(WeakHashMap<Lobby, Boolean>())
    .apply { add(defaultLobby) }
  private val mutex = Mutex()

  suspend fun getLobby(id: String) = mutex.withLock {
    lobbies.find { it.id == id } ?: Lobby().also(lobbies::add)
  }
}