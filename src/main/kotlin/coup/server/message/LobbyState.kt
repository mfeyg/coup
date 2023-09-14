package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class LobbyState(val id: String, val players: List<String>, val startingIn: Int?) : Message
