package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.game.Player.Agent.BlockResponse
import kotlinx.serialization.Serializable

class RespondToBlock(
  private val player: Player,
  blocker: Player,
  blockingInfluence: Influence
) : Prompt<BlockResponse>() {

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

  private val request = Request(
    blocker = blocker.playerNumber,
    blockingInfluence = blockingInfluence,
  )

  override val config = config(
    request = request,
    readResponse = ::read,
    validate = {}
  )

  private fun read(response: Response) = when (response.response) {
    ResponseType.Allow -> BlockResponse.Allow
    ResponseType.Challenge -> BlockResponse.Challenge(player)
  }
}