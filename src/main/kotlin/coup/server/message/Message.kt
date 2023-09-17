package coup.server.message

import coup.server.Sendable
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

sealed interface Message : Sendable {
  private val serializer get() = serializer(this::class.createType())
  override fun toFrame(): Frame = Frame.Text(this::class.simpleName + Json.encodeToString(serializer, this))

  companion object {
    private val messageTypes = Message::class.sealedSubclasses.associateBy { it.simpleName!! }
      .mapValues { (_, value) -> serializer(value.createType()) }

    fun read(message: String): Message? {
      val messagePattern = Regex("(\\w+)(\\{.*})?")
      return messagePattern.matchEntire(message)?.let { match ->
        val (_, messageType, messageContents) = match.groupValues
        val deserializer = messageTypes[messageType]
        deserializer?.let { Json.decodeFromString(deserializer, messageContents.ifEmpty { "{}" }) }
      } as Message?
    }
  }
}
