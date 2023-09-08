package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class PlayerLeft(val playerName: String) : Message
