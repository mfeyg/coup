package coup.game

sealed interface ActionResponse {
  data object Allow : ActionResponse
  data class Challenge(val challenger: Player) : ActionResponse
  data class Block(val blocker: Player, val influence: Influence) : ActionResponse
}

sealed interface BlockResponse {
  data object Allow : BlockResponse
  data class Challenge(val challenger: Player) : BlockResponse
}

data class ChallengeResponse(val influence: Influence)
