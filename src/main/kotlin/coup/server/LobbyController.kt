package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import coup.server.message.NewLobby
import java.util.*
import kotlin.ConcurrentModificationException

class LobbyController(private val createLobby: () -> Lobby) {
  private val lobbies = WeakHashMap<Lobby, String>()
  private val defaultLobby = newId

  private class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      val lobby = newLobby()
      socket.send(lobby)
      lobby.connect(socket)
    } else if (id == null) {
      val lobby = lobby(defaultLobby) ?: newLobby()
      lobbies[lobby] = defaultLobby
      socket.send(lobby)
      lobby.connect(socket)
    } else {
      val lobby = lobby(id) ?: throw LobbyNotFound(id)
      lobby.connect(socket)
    }
  }

  private suspend fun SocketConnection.send(lobby: Lobby) {
    lobbies[lobby]?.let { id -> send(NewLobby(id)) }
  }

  private fun newLobby() = createLobby().also { lobbies[it] = newId }

  private fun lobby(id: String): Lobby? = try {
    lobbies.filterValues { it == id }.keys.firstOrNull()
  } catch (e: ConcurrentModificationException) {
    lobby(id)
  }

}
