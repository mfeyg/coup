package coup.game

sealed interface Permission {
  val allowed: Boolean

  companion object {
    inline fun <reified T : Permission> allow(): T = when (T::class) {
      ActionResponse::class, ActionResponse.Allow::class -> ActionResponse.Allow as T
      BlockResponse::class, BlockResponse.Allow::class -> BlockResponse.Allow as T
      else -> throw UnsupportedOperationException()
    }
  }
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
