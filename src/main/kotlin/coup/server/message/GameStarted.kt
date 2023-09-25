package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class GameStarted(val id: String) : Message
