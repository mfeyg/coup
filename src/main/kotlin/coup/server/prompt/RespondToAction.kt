package coup.server.prompt

import coup.game.actions.Action
import coup.game.actions.Action.Type.Companion.type
import coup.game.Influence
import coup.game.Player
import coup.game.Player.Agent.ActionResponse
import coup.game.rules.Ruleset
import coup.server.prompt.Promptable.Companion.prompt
import coup.server.prompt.RespondToAction.Response.Type.*
import kotlinx.serialization.Serializable

object RespondToAction {

  @Serializable
  private data class Request(
    val player: Int,
    val type: ActionType,
    val target: Int?,
    val canBeChallenged: Boolean,
    val canBeBlocked: Boolean,
    val blockingInfluences: Set<Influence>,
    val claimedInfluence: Influence?,
  ) {
    constructor(player: Player, action: Action, ruleset: Ruleset) : this(
      player = player.playerNumber,
      type = ActionType(action.type),
      target = action.target?.playerNumber,
      canBeChallenged = ruleset.canChallenge(player, action),
      canBeBlocked = ruleset.canAttemptBlock(player, action),
      blockingInfluences = ruleset.blockingInfluences(action),
      claimedInfluence = ruleset.requiredInfluence(action),
    )
  }

  @Serializable
  private data class Response(
    val type: Type,
    val influence: Influence? = null
  ) {
    enum class Type {
      Allow, Block, Challenge
    }
  }

  suspend fun Promptable.respondToAction(player: Player, action: Action, ruleset: Ruleset): ActionResponse =
    prompt(
      "RespondToAction",
      Request(player, action, ruleset)
    ) { response: Response ->
      when (response.type) {
        Allow -> ActionResponse.Allow
        Block -> {
          val influence = requireNotNull(response.influence) { "Influence required to block." }
          require(ruleset.canAttemptBlock(player, action)) { "$player cannot block $action." }
          require(influence in ruleset.blockingInfluences(action)) { "$influence cannot block $action." }
          ActionResponse.Block(player, influence)
        }

        Challenge -> {
          require(ruleset.canChallenge(player, action)) { "$player cannot challenge $action" }
          ActionResponse.Challenge(player)
        }
      }
    }
}