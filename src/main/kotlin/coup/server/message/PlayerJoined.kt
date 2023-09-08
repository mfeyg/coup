package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class PlayerJoined(val playerName: String) : Message
