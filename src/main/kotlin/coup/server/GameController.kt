package coup.server

import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import java.util.WeakHashMap

class GameController {
  private val games = Collections.newSetFromMap(WeakHashMap<Game, Boolean>())
  private val mutex = Mutex()

  class GameNotFound : ServerError()

  suspend fun connect(connection: SocketConnection, id: String) {
    val game = game(id) ?: throw GameNotFound()
    game.connect(connection)
  }

  private suspend fun game(id: String): Game? = mutex.withLock {
    games.find { it.id == id }
  }

  suspend fun newGame(players: Iterable<Session<*>>): Game {
    val game = Game.new(players)
    mutex.withLock { games.add(game) }
    return game
  }
}
