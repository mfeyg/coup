package coup.server

import coup.game.Influence
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
  val player: Player? = null,
  val players: List<Opponent>,
  val currentTurn: Int? = null,
  val winner: Int? = null,
) {
  @Serializable
  data class Player(
    val name: String,
    val number: Int,
    val isk: Int,
    val heldInfluences: List<Influence>,
    val revealedInfluences: List<Influence>,
  ) {
    constructor(person: Person, player: coup.game.Player) : this(
      name = person.name,
      number = player.number,
      isk = player.isk,
      heldInfluences = player.heldInfluences,
      revealedInfluences = player.revealedInfluences,
    )
  }

  @Serializable
  data class Opponent(
    val name: String,
    val number: Int,
    val isk: Int,
    val heldInfluences: Int,
    val revealedInfluences: List<Influence>,
  ) {
    constructor(person: Person, player: coup.game.Player) : this(
      name = person.name,
      number = player.number,
      isk = player.isk,
      heldInfluences = player.heldInfluences.size,
      revealedInfluences = player.revealedInfluences,
    )
  }
}