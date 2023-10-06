package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import coup.server.message.NewLobby
import java.util.*
import kotlin.ConcurrentModificationException

class LobbyController(private val createLobby: () -> Lobby) {
  private val lobbies = WeakHashMap<Lobby, String>()
  private val defaultLobby = newLobby()

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

  private fun newLobby() = NewLobby(newId)

  private fun lobby(id: String): Lobby = try {
    lobbies.filterValues { it == id }.keys.firstOrNull()
      ?: createLobby().also { lobbies[it] = id }
  } catch (e: ConcurrentModificationException) {
    lobby(id)
  }
}