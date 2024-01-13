package coup.server.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
  routing {
    get("/") {
      call.respondRedirect("/lobby.html")
    }
    get("/demo") {
      call.respondRedirect("/game.html?sample")
    }
    staticResources("/", "www")
  }
}
