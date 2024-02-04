package coup.server

import io.ktor.websocket.*

class UserConnection(val user: Person, private val socket: WebSocketSession) :
  WebSocketSession by socket