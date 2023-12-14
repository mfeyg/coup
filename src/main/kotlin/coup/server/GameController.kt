package coup.server

import coup.server.ConnectionController.SocketConnection
import java.util.WeakHashMap

class GameController {
  private val games = WeakHashMap<GameServer, String>()

  class GameNotFound : ServerError()

  suspend fun connect(connection: SocketConnection, id: String) {
    val game = game(id) ?: throw GameNotFound()
    game.connect(connection)
  }

  private fun game(gameId: String): GameServer? = try {
    games.entries.find { (_, id) -> id == gameId }?.let { (game, _) -> game }
  } catch (e: ConcurrentModificationException) {
    game(gameId)
  }

  suspend fun newGame(players: Iterable<Session<*>>, lobby: Lobby): String {
    val gameServer = GameServer.new(players, lobby)
    val gameId = newId
    games[gameServer] = gameId
    return gameId
  }
}
