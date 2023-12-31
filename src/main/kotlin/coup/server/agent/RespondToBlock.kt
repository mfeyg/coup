package coup.server.agent

import coup.game.Influence
import coup.game.Reaction.Block
import kotlinx.serialization.Serializable

object RespondToBlock {

  @Serializable
  private data class Request(
    val blocker: Int,
    val blockingInfluence: Influence
  ) {
    constructor(block: Block) : this(block.blocker.number, block.blockingInfluence)
  }

  @Serializable
  private data class Response(val reaction: ResponseType)

  enum class ResponseType {
    Allow, Challenge
  }

  suspend fun PromptContext.challengeBlock(block: Block) = prompt {
    type = "RespondToBlock"
    request(
      Request(block)
    )
    readResponse { (response): Response ->
      when (response) {
        ResponseType.Allow -> false
        ResponseType.Challenge -> true
      }
    }
    timeout(options.responseTimer) { false }
  }
}