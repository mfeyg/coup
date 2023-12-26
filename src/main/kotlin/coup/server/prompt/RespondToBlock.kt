package coup.server.prompt

import coup.game.Influence
import coup.game.Reaction.Block
import coup.server.Session
import kotlinx.serialization.Serializable

class RespondToBlock(private val session: Session<*, *>) {

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
    session.prompt(
      "RespondToBlock",
    ) { (response): Response ->
      when (response) {
        ResponseType.Allow -> false
        ResponseType.Challenge -> true
      }
    }.request(
      Request(block)
    ).send()
}