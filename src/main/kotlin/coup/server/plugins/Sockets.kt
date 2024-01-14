package coup.server.plugins

import coup.server.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
import java.time.Duration

fun Application.configureSockets() {
  install(WebSockets) {
    pingPeriod = Duration.ofSeconds(15)
    timeout = Duration.ofSeconds(15)
    maxFrameSize = Long.MAX_VALUE
    masking = false
  }
  routing {
    val gameController = GameController()
    val lobbyController = LobbyController { Lobby(gameController::newGame) }
    val connectionController = ConnectionController()
    webSocket("/lobby") {
      lobbyController.connect(
        connectionController.connection(this),
        id = call.parameters["id"]?.let(::Id),
        newLobby = call.parameters.contains("new"),
      )
    }
    webSocket("/game") {
      if (call.parameters.contains("sample")) {
        val connection = connectionController.connection(this)
        val game = GameBuilder {
          addHumanPlayer(connection.user, ::LoggingAgent)
          repeat(4) { addComputerPlayer(::LoggingAgent) }
          shufflePlayers()
        }
        launch { game.start() }
        game.connect(connection)
      } else {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Game ID is required")
        gameController.connect(connectionController.connection(this), Id(id))
      }
    }
  }
}
