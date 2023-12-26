package coup.server

import kotlinx.serialization.Serializable

@Serializable
data class LobbyState(val players: List<Player>, val startingIn: Int?, val options: GameOptions) {
  @Serializable
  data class Player(val name: String, val color: String, val champion: Boolean)
}
