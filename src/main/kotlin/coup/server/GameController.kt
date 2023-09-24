package coup.server

import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.WeakHashMap

class GameController {
  private val games = WeakHashMap<Game, String>()
  private val mutex = Mutex()

  class GameNotFound : ServerError()

  suspend fun connect(connection: SocketConnection, id: String) {
    val game = game(id) ?: throw GameNotFound()
    game.connect(connection)
  }

  private suspend fun game(gameId: String): Game? = mutex.withLock {
    games.entries.find { (_, id) -> id == gameId }?.let { (game, _) -> game }
  }

  suspend fun newGame(players: Iterable<Session<*>>): String {
    val game = Game.new(players)
    val gameId = newId
    mutex.withLock { games.put(game, gameId) }
    return gameId
  }
}
