package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class GameController {
  private val games = MutableStateFlow(mapOf<String, GameServer>())

  suspend fun connect(connection: SocketConnection, id: String) {
    val game = game(id) ?: run {
      connection.send("GameNotFound")
      return
    }
    game.connect(connection)
  }

  private fun game(gameId: String) = games.value[gameId]

  suspend fun newGame(players: Iterable<Session<*>>, lobby: Lobby): String {
    val gameServer = GameServer(players, lobby)
    val gameId = newId
    games.update { it + (gameId to gameServer) }
    gameServer.onShutDown { games.update { it - gameId } }
    return gameId
  }
}
