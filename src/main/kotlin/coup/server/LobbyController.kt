package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import java.util.*

class LobbyController(private val createLobby: () -> Lobby) {
  private val lobbyIds = WeakHashMap<Lobby, String>()
  private val defaultId = newId
  private val scope = CoroutineScope(Default)

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
        socket.send("LobbyNotFound")
        return
      }
    }
    lobby.connect(socket)
  }

  private suspend fun SocketConnection.send(lobby: Lobby) {
    send("GoToLobby:" + (lobbyIds[lobby] ?: return))
  }

  private fun lobby(id: String): Lobby? = try {
    lobbyIds.filterValues { it == id }.keys.firstOrNull()?.takeIf { it.isActive }
  } catch (e: ConcurrentModificationException) {
    lobby(id)
  }

}
