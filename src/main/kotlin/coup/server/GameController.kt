package coup.server

import coup.game.rules.StandardRules
import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class GameController {
  private val games = MutableStateFlow(mapOf<Id, GameServer>())
  private val ruleset = StandardRules()

  suspend fun connect(connection: SocketConnection, id: Id) {
    game(id)?.connect(connection) ?: connection.send("GameNotFound")
  }

  private fun game(gameId: Id) = games.value[gameId]

  fun newGame(players: List<Person>, lobby: Lobby, options: GameOptions): Id {
    val gameServer = GameServer(players.take(ruleset.maxPlayers), lobby, ruleset, options)
    val gameId = Id()
    games.update { it + (gameId to gameServer) }
    gameServer.onShutDown { games.update { it - gameId } }
    gameServer.start()
    return gameId
  }
}
