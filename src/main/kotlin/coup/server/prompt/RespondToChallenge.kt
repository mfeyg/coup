package coup.server.prompt

import coup.game.ChallengeResponse
import coup.game.Influence
import coup.game.Player
import kotlinx.serialization.Serializable

class RespondToChallenge(
  claim: Influence,
  challenger: Player,
  private val heldInfluences: List<Influence>,
) : Prompt<ChallengeResponse>() {
  @Serializable
  private data class Request(val claim: Influence, val challenger: Int)

  @Serializable
  private data class Response(val influence: Influence)

  override val config = config(
    request = Request(claim, challenger.playerNumber),
    readResponse = { response: Response ->
      ChallengeResponse(response.influence)
    },
    validate = { require { it.influence in heldInfluences } }
  )

}