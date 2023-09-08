package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class LobbyUpdate(val lobby: Lobby) : Message {

  @Serializable
  data class Lobby(val id: String, val players: List<String>, val startingIn: Int?)
}
