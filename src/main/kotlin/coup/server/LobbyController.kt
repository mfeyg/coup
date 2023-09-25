package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.Sendable.Companion.send
import coup.server.message.NewLobby
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class LobbyController(private val gameController: GameController) {
  private val defaultLobby = Lobby()
  private val defaultLobbyId = newId
  private val lobbies = WeakHashMap<Lobby, String>()
    .apply { put(defaultLobby, defaultLobbyId) }
  private val mutex = Mutex()

  class LobbyNotFound(id: String) : ServerError("Lobby $id not found")

  suspend fun connect(socket: SocketConnection, id: String?, newLobby: Boolean) {
    if (newLobby) {
      val lobby = newLobby()
      socket.send(NewLobby(lobby))
      return
    } else if (id == null) {
      socket.send(NewLobby(defaultLobbyId))
      return
    }
    val lobby = findLobby(id) ?: throw LobbyNotFound(id)
    lobby.connect(socket)
  }

  private suspend fun findLobby(lobbyId: String) = mutex.withLock {
    lobbies.entries.find { (_, id) -> id == lobbyId }?.let { (lobby, _) -> lobby }
  }

  private fun Lobby() = Lobby { gameController.newGame(it, this) }

  private suspend fun newLobby() = mutex.withLock {
    val lobby = Lobby()
    val id = newId
    lobbies[lobby] = id
    id
  }
}