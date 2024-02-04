package coup.game

sealed class Reaction {
  data object Allow : Reaction()
  data class Block(val blocker: Player, val blockingInfluence: Influence): Reaction()
  data class Challenge(val challenger: Player): Reaction()
}
