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

  private fun lobby(lobbyId: String): Lobby = try {
    lobbies.entries.find { (_, id) -> id == lobbyId }?.let { (lobby, _) -> lobby }
      ?: throw LobbyNotFound(lobbyId)
  } catch (e: ConcurrentModificationException) {
    lobby(lobbyId)
  }

  private fun newLobby(): NewLobby {
    val lobby = createLobby()
    val id = newId
    lobbies[lobby] = id
    return NewLobby(id)
  }
}