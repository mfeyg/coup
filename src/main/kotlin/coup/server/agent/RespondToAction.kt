package coup.server.agent

import coup.game.Influence
import coup.game.Reaction
import coup.game.actions.Action
import coup.server.agent.RespondToAction.Response.Type.*
import coup.server.dto.ActionData
import coup.server.dto.ActionData.Companion.dto
import kotlinx.serialization.Serializable

object RespondToAction {

  @Serializable
  private data class Request(val action: ActionData)

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
      Request(action.dto(player, ruleset))
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