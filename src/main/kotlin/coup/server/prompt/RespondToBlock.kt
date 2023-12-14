package coup.server.prompt

import coup.game.Influence
import coup.game.Reaction.Block
import coup.server.prompt.Promptable.Companion.prompt
import kotlinx.serialization.Serializable

class RespondToBlock(private val promptable: Promptable) {

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

  suspend fun challengeBlock(block: Block): Boolean =
    promptable.prompt(
      "RespondToBlock",
      Request(block)
    ) { (response): Response ->
      when (response) {
        ResponseType.Allow -> false
        ResponseType.Challenge -> true
      }
    }
}