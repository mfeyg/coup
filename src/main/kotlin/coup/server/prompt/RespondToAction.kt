package coup.server.prompt

import coup.game.ActionResponse
import coup.game.Influence
import coup.game.Player
import coup.server.ServerError
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

  override fun prompt() = sendAndReceive(
    Request(
      player = action.player.playerNumber,
      type = action.type,
      target = action.target?.playerNumber,
      canBeChallenged = action.type.neededInfluence != null,
      canBeBlocked = action.canBeBlockedBy(player),
      blockingInfluences = action.type.blockingInfluences,
      claimedInfluence = action.type.neededInfluence
    )
  ) { response: Response ->
    when (response.response) {
      ResponseType.Allow -> ActionResponse.Allow
      ResponseType.Block -> ActionResponse.Block(
        player,
        response.influence ?: throw ServerError("Influence required to block")
      )

      ResponseType.Challenge -> ActionResponse.Challenge(player)
    }
  }
}