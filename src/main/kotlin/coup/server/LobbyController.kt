package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import coup.server.message.NewLobby
import java.util.*
import kotlin.ConcurrentModificationException

class LobbyController(private val createLobby: () -> Lobby) {
  private val lobbyIds = WeakHashMap<Lobby, String>()
  private val defaultId = newId

  private class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      val lobby = createLobby()
      lobbyIds[lobby] = newId
      socket.send(lobby)
      lobby.connect(socket)
    } else if (id == null) {
      val lobby = lobby(defaultId) ?: createLobby()
      lobbyIds[lobby] = defaultId
      socket.send(lobby)
      lobby.connect(socket)
    } else {
      val lobby = lobby(id) ?: throw LobbyNotFound(id)
      lobby.connect(socket)
    }
  }

  private suspend fun SocketConnection.send(lobby: Lobby) {
    lobbyIds[lobby]?.let { id -> send(NewLobby(id)) }
  }

  private fun lobby(id: String): Lobby? = try {
    lobbyIds.filterValues { it == id }.keys.firstOrNull()
  } catch (e: ConcurrentModificationException) {
    lobby(id)
  }

}
