package coup.server

import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

interface Sendable {
  fun toFrame(): Frame = Frame.Text(
    this::class.simpleName!!
            + Json.encodeToString(serializer(this::class.createType()), this)
  )

  companion object {
    suspend fun WebSocketSession.send(sendable: Sendable) = send(sendable.toFrame())
  }
}