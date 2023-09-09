package coup.server.plugins

import coup.server.GameController
import coup.server.LobbyController
import coup.server.ServerError
import coup.server.ServerError.BadRequest
import coup.server.SocketController
import coup.server.message.CancelGameStart
import coup.server.message.Error
import coup.server.message.StartGame
import coup.server.message.StartGame.send
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    val socketController = SocketController()
    val lobbyController = LobbyController()
    val gameController = GameController()
    webSocket("/lobby") {
      sendErrors {
        val lobbyId = call.parameters["lobby"]
        val lobby = if (lobbyId != null) lobbyController.getLobby(lobbyId) else lobbyController.defaultLobby
        val name = call.parameters["name"] ?: throw BadRequest("Name is required")
        socketController.withSocket(this) { socket ->
          socket.name = name
          lobby.join(socket)
          var startGameJob: Job? = null
          val listener = socket.messages.onEach { message ->
            when (message) {
              StartGame -> startGameJob = lobby.startGame(this) {
                val game = gameController.newGame(lobby.evacuate())
                game.start()
              }

              CancelGameStart -> startGameJob?.cancelAndJoin()
              else -> {}
            }
          }.launchIn(this)
          socket.connect(this)
          lobby.leave(socket)
          listener.cancelAndJoin()
        }
      }
    }
    webSocket("/game") {
      sendErrors {
        socketController.withSocket(this) { socket ->
          val id = call.parameters["id"] ?: throw BadRequest("Game ID is required")
          gameController.getGame(id) ?: throw BadRequest("Game $id not found")
          socket.connect(this)
        }
      }
    }
  }
}

suspend fun WebSocketServerSession.sendErrors(block: suspend () -> Unit) {
  try {
    block()
  } catch (e: ServerError) {
    send(Error(e.message))
    Logger.getGlobal().log(Level.WARNING, e.message, e)
  }
}
