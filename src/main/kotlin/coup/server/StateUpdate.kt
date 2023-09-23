package coup.server

import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

class StateUpdate<out State>(private val state: State) {
  companion object {
    suspend fun WebSocketSession.send(update: StateUpdate<Any>) {
      val encoded = Json.encodeToString(serializer(update.state::class.createType()), update.state)
      send(Frame.Text("State$encoded"))
    }
  }
}
