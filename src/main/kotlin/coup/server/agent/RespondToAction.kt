package coup.server.agent

import coup.game.actions.Action
import coup.game.actions.Action.Type.Companion.type
import coup.game.Influence
import coup.game.Player
import coup.game.Reaction
import coup.game.rules.Ruleset
import coup.server.agent.RespondToAction.Response.Type.*
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
      player = action.player.number,
      type = ActionType(action.type),
      target = action.target?.number,
      canBeChallenged = ruleset.canChallenge(player, action),
      canBeBlocked = ruleset.canAttemptBlock(player, action),
      blockingInfluences = ruleset.blockingInfluences(action),
      claimedInfluence = ruleset.requiredInfluence(action),
    )
  }

  @Serializable
  private data class Response(
    val reaction: Type,
    val influence: Influence? = null
  ) {
    enum class Type {
      Allow, Block, Challenge
    }
  }

  suspend fun PromptContext.respondToAction(action: Action) = prompt {
    type = "RespondToAction"
    request(
      Request(player, action, ruleset)
    )
    readResponse { (reaction, influence): Response ->
      when (reaction) {
        Allow -> Reaction.Allow

        Block -> {
          requireNotNull(influence) { "Influence required to block." }
          require(ruleset.canAttemptBlock(player, action)) { "$player cannot block $action." }
          require(influence in ruleset.blockingInfluences(action)) { "$influence cannot block $action." }
          Reaction.Block(player, influence)
        }

        Challenge -> {
          require(ruleset.canChallenge(player, action)) { "$player cannot challenge $action" }
          Reaction.Challenge(player)
        }
      }
    }
    timeout(options.responseTimer) { Reaction.Allow }
  }
}