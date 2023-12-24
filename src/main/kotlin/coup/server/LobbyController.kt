package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class LobbyController(private val newLobby: () -> Lobby) {
  private val lobbyIds = MutableStateFlow(mapOf<String, Lobby>())
  private val defaultLobbyId = newId

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      socket.send("GoToLobby:${createLobby(newId)}")
    } else if (id == null) {
      lobby(defaultLobbyId) ?: createLobby(defaultLobbyId)
      socket.send("GoToLobby:$defaultLobbyId")
    } else {
      lobby(id)?.connect(socket) ?: socket.send("LobbyNotFound:$id")
    }
  }

  private fun createLobby(id: String): String {
    val lobby = newLobby()
    lobbyIds.update { it + (id to lobby) }
    lobby.onShutDown {
      lobbyIds.update { it.filterValues { it != lobby } }
    }
    return id
  }

  private fun lobby(id: String): Lobby? = lobbyIds.value[id]?.takeIf { it.isActive }

}
