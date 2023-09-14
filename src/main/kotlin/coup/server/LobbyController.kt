package coup.server

import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class LobbyController(private val gameController: GameController) {
  private val defaultLobby = createLobby()
  private val lobbies = Collections.newSetFromMap(WeakHashMap<Lobby, Boolean>())
    .apply { add(defaultLobby) }
  private val mutex = Mutex()

  class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?) {
    val lobby = id?.let { getLobby(id) ?: throw LobbyNotFound(id) } ?: defaultLobby
    lobby.connect(socket)
  }

  private suspend fun getLobby(id: String) = mutex.withLock {
    lobbies.find { it.id == id }
  }

  private fun createLobby(): Lobby = Lobby({ gameController.newGame(it, this) }, { newLobby() })

  private suspend fun newLobby(): Lobby = mutex.withLock { createLobby().also { lobbies.add(it) } }
}