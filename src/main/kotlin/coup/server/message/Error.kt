package coup.server.message

import coup.server.ServerError
import kotlinx.serialization.Serializable

@Serializable
data class Error(val type: String, val message: String?) : Message {
  companion object {
    inline fun <reified T : ServerError> from(error: T) = Error(T::class.simpleName!!, error.message)
  }
}