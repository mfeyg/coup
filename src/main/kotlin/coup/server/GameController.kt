package coup.server

import coup.game.rules.StandardRules
import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class GameController {
  private val games = MutableStateFlow(mapOf<String, GameServer>())
  private val ruleset = StandardRules()

  suspend fun connect(connection: SocketConnection, id: String) {
    game(id)?.connect(connection) ?: connection.send("GameNotFound")
  }

  private fun game(gameId: String) = games.value[gameId]

  suspend fun newGame(players: List<Person>, lobby: Lobby, options: GameOptions): String {
    val gameServer = GameServer(players.take(ruleset.maxPlayers), lobby, ruleset, options)
    val gameId = newId
    games.update { it + (gameId to gameServer) }
    gameServer.onShutDown { games.update { it - gameId } }
    gameServer.start()
    return gameId
  }
}
