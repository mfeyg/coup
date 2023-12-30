package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class GameController {
  private val games = MutableStateFlow(mapOf<Id, GameServer>())

  suspend fun connect(connection: SocketConnection, gameId: Id) {
    game(gameId)?.connect(connection) ?: connection.send("GameNotFound")
  }

  private fun game(id: Id) = games.value[id]

  fun newGame(players: List<Person>, gameOptions: GameOptions): Pair<GameServer, Id> {
    val gameServer = GameServer {
      options = gameOptions
      players.forEach { player ->
        addHumanPlayer(player)
      }
    }
    val id = Id()
    games.update { it + (id to gameServer) }
    gameServer.onShutDown { games.update { it - id } }
    gameServer.start()
    return gameServer to id
  }
}
