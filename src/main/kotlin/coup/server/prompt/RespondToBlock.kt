package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.game.Player.Agent.BlockResponse
import coup.server.prompt.Promptable.Companion.prompt
import kotlinx.serialization.Serializable

object RespondToBlock {

  @Serializable
  private data class Request(
    val blocker: Int,
    val blockingInfluence: Influence
  )

  @Serializable
  private data class Response(val response: ResponseType)

  enum class ResponseType {
    Allow, Challenge
  }

  suspend fun Promptable.respondToBlock(blocker: Player, blockingInfluence: Influence): BlockResponse =
    prompt(
      "RespondToBlock",
      Request(blocker.playerNumber, blockingInfluence)
    ) { (response): Response ->
      when (response) {
        ResponseType.Allow -> BlockResponse.Allow
        ResponseType.Challenge -> BlockResponse.Challenge
      }
    }
}