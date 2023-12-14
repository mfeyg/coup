package coup.server.prompt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.NothingSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

interface Promptable {
  suspend fun <T, RequestT, ResponseT> prompt(
    promptType: String,
    request: RequestT,
    requestSerializer: KSerializer<RequestT>,
    responseSerializer: KSerializer<ResponseT>,
    readResponse: (ResponseT) -> T
  ): T

  companion object {
    suspend inline fun <T, reified RequestT, reified ResponseT> Promptable.prompt(
      promptType: String,
      request: RequestT,
      noinline readResponse: (ResponseT) -> T
    ) = prompt(promptType, request, serializer(), serializer(), readResponse)

    suspend inline fun <T, reified ResponseT> Promptable.prompt(
      promptType: String,
      noinline readResponse: (ResponseT) -> T
    ) = prompt(promptType, null, NothingSerializer().nullable, serializer(), readResponse)
  }
}