package coup.server.plugins

import coup.server.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
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
        id = call.parameters["id"],
        newLobby = call.parameters.contains("new"),
      )
    }
    webSocket("/game") {
      val id = call.parameters["id"] ?: throw IllegalArgumentException("Game ID is required")
      gameController.connect(connectionController.connection(this), id)
    }
  }
}
