package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import coup.server.message.NewLobby
import java.util.*
import kotlin.ConcurrentModificationException

class LobbyController(private val createLobby: () -> Lobby) {
  private val defaultLobby = createLobby()
  private val defaultLobbyId = newId
  private val lobbies = WeakHashMap<Lobby, String>()
    .apply { put(defaultLobby, defaultLobbyId) }

  class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      socket.send(NewLobby(newLobby()))
      return
    } else if (id == null) {
      socket.send(NewLobby(defaultLobbyId))
      return
    }
    val lobby = findLobby(id) ?: throw LobbyNotFound(id)
    lobby.connect(socket)
  }

  private fun findLobby(lobbyId: String): Lobby? = try {
    lobbies.entries.find { (_, id) -> id == lobbyId }?.let { (lobby, _) -> lobby }
  } catch (e: ConcurrentModificationException) {
    findLobby(lobbyId)
  }

  private fun newLobby(): String {
    val lobby = createLobby()
    val id = newId
    lobbies[lobby] = id
    return id
  }
}