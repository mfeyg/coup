package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class LobbyCreated(val id: String) : Message