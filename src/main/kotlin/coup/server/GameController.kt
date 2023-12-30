package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class GameController {
  private val games = MutableStateFlow(mapOf<Id, GameServer>())

  suspend fun connect(connection: SocketConnection, id: Id) {
    game(id)?.connect(connection) ?: connection.send("GameNotFound")
  }

  private fun game(gameId: Id) = games.value[gameId]

  fun newGame(players: List<Person>, lobby: Lobby, gameOptions: GameOptions): Id {
    val gameServer = GameServer {
      options = gameOptions
      players.forEach { player ->
        addHumanPlayer(player)
      }
    }
    gameServer.onComplete { game ->
      game.winner?.let { winner ->
        lobby.setChampion(players[winner.number])
      }
    }
    val id = Id()
    games.update { it + (id to gameServer) }
    gameServer.onShutDown { games.update { it - id } }
    gameServer.start()
    return id
  }
}
