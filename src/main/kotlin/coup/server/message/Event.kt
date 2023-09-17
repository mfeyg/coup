package coup.server.message

import coup.game.GameEvent
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

class Event(private val event: GameEvent) {
  companion object {
    suspend fun WebSocketSession.send(event: Event) {
      val eventType = event.event::class.simpleName!!
      val payload = Json.encodeToString(serializer(event::class.createType()), event.event)
      val frame = Frame.Text("""Event{"type":"$eventType","event":$payload}""")
      send(frame)
    }
  }
}