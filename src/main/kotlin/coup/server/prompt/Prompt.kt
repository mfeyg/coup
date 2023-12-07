package coup.server.prompt

import coup.server.ServerError
import coup.server.newId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

abstract class Prompt<T> {
  val id = newId
  val request get() = config.request
  protected abstract val config: Config<T>
  fun parse(response: String) = config.parse(response)

  companion object {
    class ValidationError(requirement: String?) :
      ServerError(if (requirement != null) "Validation failed: $requirement" else "Validation failed")

    fun require(requirement: String? = null, check: () -> Boolean) {
      if (!check()) throw ValidationError(requirement)
    }
  }

  protected inline fun <reified RequestT, reified ResponseT> config(
    request: RequestT,
    noinline readResponse: (ResponseT) -> T,
    noinline validate: (T) -> Unit = {},
  ) = config(
    Json.encodeToString(request),
    { readResponse(Json.decodeFromString(it)) },
    validate
  )

  protected inline fun <reified ResponseT> config(
    noinline readResponse: (ResponseT) -> T,
    noinline validate: (T) -> Unit,
  ) = config(
    "",
    { readResponse(Json.decodeFromString(it)) },
    validate
  )

  private val promptType = this::class.simpleName!!

  protected fun config(request: String, readResponse: (String) -> T, validateResponse: (T) -> Unit) = Config(
    promptType,
    id,
    request,
    readResponse,
    validateResponse
  )

  protected class Config<T>(
    type: String,
    id: String,
    request: String,
    private val readResponse: (String) -> T,
    private val validateResponse: (T) -> Unit,
  ) {
    val request = "$type[$id]$request"
    fun parse(response: String) = readResponse(response).also(validateResponse)
  }
}
