package coup.server

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface Prompt {
  suspend fun <T> prompt(promptType: String, request: String = "", readResponse: (String) -> T): T

  companion object {
    suspend inline fun <T, reified RequestT, reified ResponseT> Prompt.prompt(
      promptType: String,
      request: RequestT,
      noinline readResponse: (ResponseT) -> T
    ) = prompt(promptType, Json.encodeToString(request)) { response ->
      readResponse(Json.decodeFromString(response))
    }

    suspend inline fun <T, reified ResponseT> Prompt.prompt(
      promptType: String,
      noinline readResponse: (ResponseT) -> T
    ) = prompt(promptType) { response ->
      readResponse(Json.decodeFromString(response))
    }
  }
}