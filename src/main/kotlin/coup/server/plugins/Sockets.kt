package coup.server.plugins

import coup.server.*
import coup.server.ServerError.BadRequest
import coup.server.message.Error
import coup.server.message.StartGame.send
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

fun Application.configureSockets() {
  install(WebSockets) {
    pingPeriod = Duration.ofSeconds(15)
    timeout = Duration.ofSeconds(15)
    maxFrameSize = Long.MAX_VALUE
    masking = false
  }
  routing {
    val gameController = GameController()
    val lobbyController = LobbyController(gameController)
    val connectionController = ConnectionController()
    webSocket("/lobby") {
      sendErrors {
        lobbyController.connect(
          connectionController.connection(this),
          id = call.parameters["id"],
        )
      }
    }
    webSocket("/game") {
      sendErrors {
        val id = call.parameters["id"] ?: throw BadRequest("Game ID is required")
        gameController.connect(connectionController.connection(this), id)
      }
    }
  }
}

suspend fun WebSocketServerSession.sendErrors(block: suspend () -> Unit) {
  try {
    block()
  } catch (e: ServerError) {
    send(Error.from(e))
    Logger.getGlobal().log(Level.WARNING, e.message, e)
  }
}
