package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import coup.server.message.NewLobby
import java.util.*
import kotlin.ConcurrentModificationException

class LobbyController(private val createLobby: () -> Lobby) {
  private val defaultLobby = NewLobby(newId)
  private val _defaultLobby = createLobby()
  private val lobbies = WeakHashMap<Lobby, String>()
    .apply { put(_defaultLobby, defaultLobby.id) }

  class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      socket.send(newLobby())
      return
    } else if (id == null) {
      socket.send(defaultLobby)
      return
    }
    lobby(id).connect(socket)
  }

  private fun lobby(id: String): Lobby = try {
    lobbies.filterValues { it == id }.keys.firstOrNull()
      ?: throw LobbyNotFound(id)
  } catch (e: ConcurrentModificationException) {
    lobby(id)
  }

  private fun newLobby() =
    NewLobby(newId).also { lobbies[createLobby()] = it.id }
}