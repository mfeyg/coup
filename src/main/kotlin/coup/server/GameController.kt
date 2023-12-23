package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import java.util.WeakHashMap

class GameController {
  private val games = WeakHashMap<GameServer, String>()

  suspend fun connect(connection: SocketConnection, id: String) {
    val game = game(id) ?: run {
      connection.send(Frame.Text("GameNotFound"))
      return
    }
    game.connect(connection)
  }

  private fun game(gameId: String): GameServer? = try {
    games.entries.find { (_, id) -> id == gameId }?.let { (game, _) -> game }
  } catch (e: ConcurrentModificationException) {
    game(gameId)
  }

  suspend fun newGame(players: Iterable<Session<*>>, lobby: Lobby): String {
    val gameServer = GameServer(players, lobby)
    val gameId = newId
    games[gameServer] = gameId
    return gameId
  }
}
