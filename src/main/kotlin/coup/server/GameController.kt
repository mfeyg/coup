package coup.server

import coup.server.ConnectionController.SocketConnection
import java.util.WeakHashMap

class GameController {
  private val games = WeakHashMap<Game, String>()

  class GameNotFound : ServerError()

  suspend fun connect(connection: SocketConnection, id: String) {
    val game = game(id) ?: throw GameNotFound()
    game.connect(connection)
  }

  private fun game(gameId: String): Game? = try {
    games.entries.find { (_, id) -> id == gameId }?.let { (game, _) -> game }
  } catch (e: ConcurrentModificationException) {
    game(gameId)
  }

  suspend fun newGame(players: Iterable<Session<*>>, lobby: Lobby): String {
    val game = Game.new(players, lobby)
    val gameId = newId
    games[game] = gameId
    return gameId
  }
}
