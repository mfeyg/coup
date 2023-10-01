package coup.server.prompt

import coup.game.Action
import coup.game.ActionResponse
import coup.game.Influence
import coup.game.Player
import coup.server.prompt.Prompt.Companion.ValidationError
import kotlinx.serialization.Serializable
import coup.game.Action.Type as ActionType

class RespondToAction(private val player: Player, action: Action) : Prompt<ActionResponse>() {
  @Serializable
  private data class Request(
    val player: Int,
    val type: ActionType,
    val target: Int?,
    val canBeChallenged: Boolean,
    val canBeBlocked: Boolean,
    val blockingInfluences: Set<Influence>,
    val claimedInfluence: Influence?,
  )

  @Serializable
  private data class Response(
    val response: ResponseType,
    val influence: Influence? = null
  )

  private enum class ResponseType {
    Allow, Block, Challenge
  }

  private val canBeChallenged = action.type.neededInfluence != null

  private val canBeBlocked = action.canBeBlockedBy(player)

  private val blockingInfluences = action.type.blockingInfluences

  private val request = Request(
    player = action.player.playerNumber,
    type = action.type,
    target = action.target?.playerNumber,
    canBeChallenged = canBeChallenged,
    canBeBlocked = canBeBlocked,
    blockingInfluences = blockingInfluences,
    claimedInfluence = action.type.neededInfluence
  )

  override val config = config(
    request = request,
    readResponse = ::read,
    validate = ::validate
  )

  private fun read(response: Response) = when (response.response) {
    ResponseType.Allow -> ActionResponse.Allow
    ResponseType.Block -> ActionResponse.Block(
      player,
      response.influence ?: throw ValidationError("Influence required to block")
    )

    ResponseType.Challenge -> ActionResponse.Challenge(player)
  }

  private fun validate(response: ActionResponse) {
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