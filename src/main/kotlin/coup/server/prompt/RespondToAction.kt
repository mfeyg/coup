package coup.server.prompt

import coup.game.Action
import coup.game.Influence
import coup.game.Player
import coup.game.Player.Agent.ActionResponse
import coup.game.Ruleset
import coup.game.Ruleset.Companion.type
import coup.server.prompt.Prompt.Companion.ValidationError
import kotlinx.serialization.Serializable

class RespondToAction(ruleset: Ruleset, private val player: Player, action: Action) : Prompt<ActionResponse>() {

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

  private val canBeChallenged = ruleset.canChallenge(player, action)

  private val canBeBlocked = ruleset.canAttemptBlock(player, action)

  private val blockingInfluences = ruleset.blockingInfluences(action)

  private val request = Request(
    player = action.player.playerNumber,
    type = ActionType(action.type),
    target = action.target?.playerNumber,
    canBeChallenged = canBeChallenged,
    canBeBlocked = canBeBlocked,
    blockingInfluences = blockingInfluences,
    claimedInfluence = ruleset.requiredInfluence(action),
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