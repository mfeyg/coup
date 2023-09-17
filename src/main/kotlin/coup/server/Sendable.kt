package coup.server

import io.ktor.websocket.*

interface Sendable {
  fun toFrame(): Frame

  companion object {
    suspend fun WebSocketSession.send(sendable: Sendable) = send(sendable.toFrame())
  }
}