package coup.server

import coup.game.GameEvent
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

class Event(private val event: GameEvent) : Sendable {
  override fun toFrame(): Frame {
    val eventType = event::class.simpleName!!
    val payload = Json.encodeToString(serializer(event::class.createType()), event)
    return Frame.Text("""Event{"type":"$eventType","event":$payload}""")
  }
}