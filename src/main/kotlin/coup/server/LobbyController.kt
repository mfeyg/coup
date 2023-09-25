package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import coup.server.message.NewLobby
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class LobbyController(private val gameController: GameController) {
  private val defaultLobby = createLobby()
  private val lobbies = Collections.newSetFromMap(WeakHashMap<Lobby, Boolean>())
    .apply { add(defaultLobby) }
  private val mutex = Mutex()

  class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      val lobby = newLobby()
      socket.send(NewLobby(lobby.id))
      return
    } else if (id == null) {
      socket.send(NewLobby(defaultLobby.id))
      return
    }
    val lobby = getLobby(id) ?: throw LobbyNotFound(id)
    lobby.connect(socket)
  }

  private suspend fun getLobby(id: String) = mutex.withLock {
    lobbies.find { it.id == id }
  }

  private fun createLobby(): Lobby = Lobby { gameController.newGame(it) }

  private suspend fun newLobby(): Lobby = mutex.withLock { createLobby().also { lobbies.add(it) } }
}