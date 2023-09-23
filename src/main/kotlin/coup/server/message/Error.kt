package coup.server.message

import coup.server.ServerError
import kotlinx.serialization.Serializable

@Serializable
data class Error(val type: String, val message: String?) : Message {
  companion object {
    fun from(error: ServerError) = Error(error::class.simpleName!!, error.message)
  }
}