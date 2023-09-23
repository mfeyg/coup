package coup.server

import coup.game.Influence
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
  val player: Player?,
  val players: List<Opponent>,
  val currentTurn: Int
) {
  @Serializable
  data class Player (
    val name: String,
    val color: String,
    val number: Int,
    val isk: Int,
    val heldInfluences: List<Influence>,
    val revealedInfluences: List<Influence>,
  )

  @Serializable
  data class Opponent(
    val name: String,
    val color: String,
    val number: Int,
    val isk: Int,
    val heldInfluences: Int,
    val revealedInfluences: List<Influence>,
  )
}