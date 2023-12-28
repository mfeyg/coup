package coup.server

import coup.server.PromptWithTimeout.TimeoutOption
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class PromptBuilder<T> {
  @Serializable
  private data class PromptRequest<T>(
    val type: String? = null,
    val id: String,
    val prompt: T? = null,
    val timeout: Int? = null
  )

  var type: String? = null
  var readResponse: ((String) -> T)? = null
  private val id = newId
  private var prompt: (timer: Int?) -> String = { Json.encodeToString(PromptRequest(type, id.value, null, it)) }

  private var timeoutOption: TimeoutOption<T>? = null

  fun <RequestT> request(request: RequestT, serializer: KSerializer<RequestT>) {
    prompt = { Json.encodeToString(PromptRequest.serializer(serializer), PromptRequest(type, id.value, request, it)) }
  }

  inline fun <reified T> request(request: T) = request(request, serializer())

  inline fun <reified ResponseT> readResponse(noinline read: (ResponseT) -> T) {
    readResponse = { read(Json.decodeFromString(it)) }
  }

  fun timeout(timeout: Int?, defaultValue: () -> T) {
    timeoutOption = timeout?.let { TimeoutOption(timeout, defaultValue()) }
  }

  private fun toPrompt() = PromptWithTimeout(id, prompt, readResponse!!, timeoutOption)

  companion object {
    fun <T> prompt(build: PromptBuilder<T>.() -> Unit) = PromptBuilder<T>().also(build).toPrompt()
  }
}