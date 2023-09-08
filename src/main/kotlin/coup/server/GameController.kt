package coup.server

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GameController {
  private val mutex = Mutex()
  private val games: MutableMap<String, Game> = mutableMapOf()

  suspend fun getGame(id: String) = mutex.withLock { games[id] }

  suspend fun newGame(players: List<Socket>) = mutex.withLock {
    Game(players).also { games[it.id] = it }
  }
}
