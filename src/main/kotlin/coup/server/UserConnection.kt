package coup.server

import io.ktor.websocket.*

class UserConnection(private val socket: WebSocketSession, val user: Person) :
  WebSocketSession by socket