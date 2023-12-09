package coup.server.prompt

import coup.game.Block
import coup.game.Influence
import coup.game.Player.Agent.BlockResponse
import coup.server.prompt.Promptable.Companion.prompt
import kotlinx.serialization.Serializable

object RespondToBlock {

  @Serializable
  private data class Request(
    val blocker: Int,
    val blockingInfluence: Influence
  ) {
    constructor(block: Block) : this(block.blocker.playerNumber, block.blockingInfluence)
  }

  @Serializable
  private data class Response(val response: ResponseType)

  enum class ResponseType {
    Allow, Challenge
  }

  suspend fun Promptable.respondToBlock(block: Block): BlockResponse =
    prompt(
      "RespondToBlock",
      Request(block)
    ) { (response): Response ->
      when (response) {
        ResponseType.Allow -> BlockResponse.Allow
        ResponseType.Challenge -> BlockResponse.Challenge
      }
    }
}