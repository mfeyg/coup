package coup.server.prompt

import coup.game.ChallengeResponse
import coup.game.Influence
import coup.game.Player
import kotlinx.serialization.Serializable

class RespondToChallenge(
  private val claim: Influence,
  private val challenger: Player
) : Prompt<ChallengeResponse>() {
  @Serializable
  data class Request(val claim: Influence, val challenger: Int)

  @Serializable
  data class Response(val influence: Influence)

  override fun prompt() =
    sendAndReceive(Request(claim, challenger.playerNumber)) { response: Response ->
      ChallengeResponse(response.influence)
    }
}