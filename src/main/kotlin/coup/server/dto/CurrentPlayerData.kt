package coup.server.dto

import coup.game.Influence
import coup.game.Player
import kotlinx.serialization.Serializable

@Serializable
data class CurrentPlayerData(
  val name: String,
  val number: Int,
  val isk: Int,
  val heldInfluences: List<Influence>,
  val revealedInfluences: List<Influence>,
) {
  companion object {
    fun Player.asCurrentPlayer() = CurrentPlayerData(
      name = name,
      number = number,
      isk = isk,
      heldInfluences = heldInfluences,
      revealedInfluences = revealedInfluences,
    )
  }
}
