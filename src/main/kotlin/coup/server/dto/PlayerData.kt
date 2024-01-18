package coup.server.dto

import coup.game.Influence
import coup.game.Player
import kotlinx.serialization.Serializable

@Serializable
data class PlayerData(
  val name: String,
  val number: Int,
  val isk: Int,
  val heldInfluences: Int,
  val revealedInfluences: List<Influence>,
) {
  companion object {
    fun Player.dto() = PlayerData(
      name = name,
      number = number,
      isk = isk,
      heldInfluences = heldInfluences.size,
      revealedInfluences = revealedInfluences,
    )
  }
}
