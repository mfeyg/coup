package coup.server.agent

import coup.game.Influence
import coup.game.Reaction.Block
import coup.server.dto.PlayerData
import coup.server.dto.PlayerData.Companion.dto
import kotlinx.serialization.Serializable

object RespondToBlock {

  @Serializable
  private data class Request(
    val blocker: PlayerData,
    val influence: Influence
  ) {
    constructor(block: Block) : this(block.blocker.dto(), block.blockingInfluence)
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