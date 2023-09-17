package coup.server.prompt

import coup.game.ActionResponse
import coup.game.Influence
import coup.game.Player
import kotlinx.serialization.Serializable
import coup.game.Action.Type as ActionType

class RespondToAction(private val player: Player, private val action: coup.game.Action) : Prompt<ActionResponse>() {
  @Serializable
  data class Request(
    val player: Int,
    val type: ActionType,
    val target: Int?,
    val canBeChallenged: Boolean,
    val canBeBlocked: Boolean,
    val blockingInfluences: Set<Influence>,
    val claimedInfluence: Influence?,
  )

  enum class ResponseType {
    Allow, Block, Challenge
  }

  @Serializable
  data class Response(val response: ResponseType, val influence: Influence? = null)

  private val canBeChallenged = action.type.neededInfluence != null

  private val canBeBlocked = action.canBeBlockedBy(player)

  private val blockingInfluences = action.type.blockingInfluences

  override fun prompt() = sendAndReceive(
    Request(
      player = action.player.playerNumber,
      type = action.type,
      target = action.target?.playerNumber,
      canBeChallenged = canBeChallenged,
      canBeBlocked = canBeBlocked,
      blockingInfluences = blockingInfluences,
      claimedInfluence = action.type.neededInfluence
    )
  ) { response: Response ->
    when (response.response) {
      ResponseType.Allow -> ActionResponse.Allow
      ResponseType.Block -> ActionResponse.Block(
        player,
        response.influence ?: throw ValidationError("Influence required to block")
      )

      ResponseType.Challenge -> ActionResponse.Challenge(player)
    }
  }

  override fun validate(response: ActionResponse) {
    when (response) {
      ActionResponse.Allow -> {}
      is ActionResponse.Block -> {
        require { canBeBlocked }
        require { response.influence in blockingInfluences }
      }

      is ActionResponse.Challenge -> require { canBeChallenged }
    }
  }
}