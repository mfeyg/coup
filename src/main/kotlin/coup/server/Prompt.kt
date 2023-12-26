package coup.server

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Integer.min
import kotlin.time.Duration.Companion.seconds

class Prompt<T>(
  val id: String,
  promptMessage: (timeout: Int?) -> String,
  private val readResponse: (String) -> T,
  private val timeoutOption: TimeoutOption<T>?,
) {
  data class TimeoutOption<T>(val timeout: Int, val defaultValue: T)

  private val timeout = MutableStateFlow(timeoutOption?.timeout)
  private val response = CompletableDeferred<T>()
  val prompt = this.timeout.map(promptMessage)

  fun complete(value: String) {
    response.complete(readResponse(value))
  }

  suspend fun await(): T = coroutineScope {
    launch timeout@{
      val (_, defaultValue) = timeoutOption ?: return@timeout
      timeout.collectLatest timeouts@{ value ->
        if (value == null) return@timeouts
        if (value == 0) response.complete(defaultValue)
        else {
          delay(1.seconds)
          timeout.update { it?.let { min(it, value - 1) } }
        }
      }
    }.let { job -> launch { response.join(); job.cancel() } }
    response.await()
  }
}