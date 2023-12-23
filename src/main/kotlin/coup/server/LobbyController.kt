package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import java.util.*

class LobbyController(private val createLobby: () -> Lobby) {
  private val lobbyIds = WeakHashMap<Lobby, String>()
  private val defaultId = newId

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    val lobby: Lobby
    if (newLobby) {
      lobby = createLobby()
      lobbyIds[lobby] = newId
      socket.send(lobby)
    } else if (id == null) {
      lobby = lobby(defaultId) ?: createLobby()
      lobbyIds[lobby] = defaultId
      socket.send(lobby)
    } else {
      lobby = lobby(id) ?: run {
        socket.send(Frame.Text("LobbyNotFound"))
        return
      }
    }
    lobby.connect(socket)
  }

  private suspend fun SocketConnection.send(lobby: Lobby) {
    @Serializable
    data class GoToLobby(val id: String) : Sendable
    send(GoToLobby(lobbyIds[lobby] ?: return))
  }

  private fun lobby(id: String): Lobby? = try {
    lobbyIds.filterValues { it == id }.keys.firstOrNull()
  } catch (e: ConcurrentModificationException) {
    lobby(id)
  }

}
