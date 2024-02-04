package coup.server.dto

import coup.server.GameOptions
import kotlinx.serialization.Serializable

@Serializable
data class LobbyState(val players: List<Player>, val startingIn: Int? = null, val options: GameOptions) {
  @Serializable
  data class Player(val name: String, val color: String, val champion: Boolean = false)
}
