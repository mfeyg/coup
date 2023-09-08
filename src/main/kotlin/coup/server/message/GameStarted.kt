package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class GameStarted(val gameId: String) : Message
