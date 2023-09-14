package coup.server.message

import kotlinx.serialization.Serializable

@Serializable
data class LobbyState(val id: String, val players: List<Player>, val startingIn: Int?) : Message {
  @Serializable
  data class Player(val name: String, val color: String)
}
