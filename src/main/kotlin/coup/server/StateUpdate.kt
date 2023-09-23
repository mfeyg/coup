package coup.server

import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

class StateUpdate<State : Any>(private val state: State) : Sendable {
  override fun toFrame(): Frame {
    val encoded = Json.encodeToString(serializer(state::class.createType()), state)
    return Frame.Text("State$encoded")
  }
}
