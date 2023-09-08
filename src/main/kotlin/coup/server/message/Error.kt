package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class Error(val message: String?) : Message {
}