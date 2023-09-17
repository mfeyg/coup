package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class Event(val description: String) : Message