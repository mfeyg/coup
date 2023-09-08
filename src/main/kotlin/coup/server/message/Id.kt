package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class Id(val id: String) : Message
