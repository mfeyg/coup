package coup.server.prompt

import coup.server.ServerError
import coup.server.newId
import io.ktor.websocket.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

sealed class Prompt<T> {
  val id = newId
  private val strategy: Strategy<*, T> by lazy { prompt() }

  private val requestFrame: Frame get() = Frame.Text(strategy.request)
  fun readResponse(response: String) = strategy.readResponse(response).also { validate(it) }

  protected abstract fun prompt(): Strategy<*, T>

  protected abstract fun validate(response: T)

  class ValidationError(requirement: String?) :
    ServerError(if (requirement != null) "Validation failed: $requirement" else "Validation failed")

  protected fun require(requirement: String? = null, check: () -> Boolean) {
    if (!check()) throw ValidationError(requirement)
  }

  protected inline fun <reified RequestT, reified ResponseT> sendAndReceive(
    request: RequestT?,
    noinline readResponse: (ResponseT) -> T
  ): Strategy<*, T> {
    val promptType = this::class.simpleName!!
    return Strategy(
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

  class Strategy<ResponseT, ValueT>(
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

  companion object {
    suspend fun WebSocketSession.send(prompt: Prompt<*>) = send(prompt.requestFrame)
  }
}
