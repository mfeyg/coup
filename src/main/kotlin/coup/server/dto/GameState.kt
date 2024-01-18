package coup.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
  val player: CurrentPlayerData? = null,
  val players: List<PlayerData>,
  val currentTurn: Int? = null,
  val winner: Int? = null,
)