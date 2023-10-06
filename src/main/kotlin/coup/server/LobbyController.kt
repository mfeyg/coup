package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import kotlinx.serialization.Serializable
import java.util.*

class LobbyController(private val createLobby: () -> Lobby) {
  private val lobbyIds = WeakHashMap<Lobby, String>()
  private val defaultId = newId

  private class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      val lobby = createLobby()
      lobbyIds[lobby] = newId
      socket.redirect(lobby)
      return
    } else if (id == null) {
      val lobby = lobby(defaultId) ?: createLobby()
      lobbyIds[lobby] = defaultId
      socket.redirect(lobby)
      return
    } else {
      val lobby = lobby(id) ?: throw LobbyNotFound(id)
      lobby.connect(socket)
    }
  }

  private suspend fun SocketConnection.redirect(lobby: Lobby) {
    @Serializable
    data class LobbyId(val id: String) : Sendable
    lobbyIds[lobby]?.let { id -> send(LobbyId(id)) }
  }

  private fun lobby(id: String): Lobby? = try {
    lobbyIds.filterValues { it == id }.keys.firstOrNull()
  } catch (e: ConcurrentModificationException) {
    lobby(id)
  }

}
