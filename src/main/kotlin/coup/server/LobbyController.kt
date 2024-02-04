package coup.server

import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class LobbyController(private val newLobby: () -> Lobby) {
  private val lobbyIds = MutableStateFlow(mapOf<Id, Lobby>())
  private val defaultLobbyId = Id()

  suspend fun connect(socket: UserConnection, id: Id?, newLobby: Boolean) {
    if (newLobby) {
      socket.send("GoToLobby:${createLobby(Id()).value}")
    } else if (id == null) {
      lobby(defaultLobbyId) ?: createLobby(defaultLobbyId)
      socket.send("GoToLobby:${defaultLobbyId.value}")
    } else {
      lobby(id)?.connect(socket) ?: socket.send("LobbyNotFound:$id")
    }
  }

  private fun createLobby(id: Id): Id {
    val lobby = newLobby()
    lobbyIds.update { it + (id to lobby) }
    lobby.onShutDown {
      lobbyIds.update { it.filterValues { it != lobby } }
    }
    lobby.start()
    return id
  }

  private fun lobby(id: Id): Lobby? = lobbyIds.value[id]?.takeIf { it.isActive }

}
