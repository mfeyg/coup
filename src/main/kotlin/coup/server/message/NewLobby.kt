package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class NewLobby(val id: String) : Message