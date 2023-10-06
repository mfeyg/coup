package coup.game

interface Permission {
  val allowed: Boolean
}

sealed class ActionResponse(override val allowed: Boolean = false) : Permission {
  data object Allow : ActionResponse(allowed = true)
  data class Challenge(val challenger: Player) : ActionResponse()
  data class Block(val blocker: Player, val influence: Influence) : ActionResponse()
}

sealed class BlockResponse(override val allowed: Boolean = false) : Permission {
  data object Allow : BlockResponse(allowed = true)
  data class Challenge(val challenger: Player) : BlockResponse()
}

data class ChallengeResponse(val influence: Influence)
