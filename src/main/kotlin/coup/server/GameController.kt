package coup.server

import coup.server.ConnectionController.SocketConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class GameController {
  private val games = MutableStateFlow(mapOf<String, Game>())

  class GameNotFound : ServerError("Game not found")

  suspend fun connect(connection: SocketConnection, id: String) {
    val game = games.value[id] ?: throw GameNotFound()
    game.connect(connection)
  }

  fun newGame(players: Iterable<Session<*>>, lobby: Lobby): Game {
    val game = Game(players, lobby)
    games.update { it + (game.id to game) }
    return game
  }
}
