package coup.server.prompt

import coup.server.newId
import io.ktor.websocket.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

sealed class Prompt<T> {
  val id = newId
  private val config: PromptStrategy<*, T> by lazy { prompt() }

  val requestFrame: Frame get() = Frame.Text(config.request)
  fun readResponse(response: String) = config.readResponse(response)

  protected abstract fun prompt(): PromptStrategy<*, T>

  protected inline fun <reified RequestT, reified ResponseT> sendAndReceive(
    request: RequestT?,
    noinline readResponse: (ResponseT) -> T
  ): PromptStrategy<*, T> {
    val promptType = this::class.simpleName!!
    return PromptStrategy(
      promptType,
      id,
      request?.let { Json.encodeToString(serializer<RequestT>(), request) },
      serializer<ResponseT>(),
      readResponse
    )
  }

  protected inline fun <reified ResponseT> sendAndReceive(
    noinline readResponse: (ResponseT) -> T
  ) = sendAndReceive<Void, ResponseT>(null, readResponse)

  class PromptStrategy<ResponseT, ValueT>(
    promptType: String,
    id: String,
    serializedRequest: String?,
    private val deserializer: DeserializationStrategy<ResponseT>,
    private val read: (ResponseT) -> ValueT,
  ) {
    val request: String = "${promptType}[${id}]" + (serializedRequest ?: "")

    fun readResponse(text: String): ValueT {
      return read(Json.decodeFromString(deserializer, text))
    }
  }
}
