package coup.server.prompt

import coup.game.Influence
import coup.game.Reaction.Block
import coup.server.PromptBuilder.Companion.prompt
import kotlinx.serialization.Serializable

class RespondToBlock(private val timeout: Int?) {

  @Serializable
  private data class Request(
    val blocker: Int,
    val blockingInfluence: Influence
  ) {
    constructor(block: Block) : this(block.blocker.playerNumber, block.blockingInfluence)
  }

  @Serializable
  private data class Response(val reaction: ResponseType)

  enum class ResponseType {
    Allow, Challenge
  }

  fun challengeBlock(block: Block) = prompt {
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
    timeout(timeout) { false }
  }
}