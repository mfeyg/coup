package coup.server.prompt

import coup.game.BlockResponse
import coup.game.Influence
import coup.game.Player
import kotlinx.serialization.Serializable

class RespondToBlock(
  private val player: Player,
  private val blocker: Player,
  private val blockingInfluence: Influence
) : Prompt<BlockResponse>() {
  @Serializable
  data class Request(val blocker: Int, val blockingInfluence: Influence)

  enum class ResponseType {
    Allow, Challenge
  }

  @Serializable
  data class Response(val response: ResponseType)

  override fun prompt() =
    sendAndReceive(Request(blocker.playerNumber, blockingInfluence)) { response: Response ->
      when (response.response) {
        ResponseType.Allow -> BlockResponse.Allow
        ResponseType.Challenge -> BlockResponse.Challenge(player)
      }
    }

  override fun validate(response: BlockResponse) {}
}