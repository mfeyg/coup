package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.game.Player.Agent.ChallengeResponse
import coup.server.prompt.Promptable.Companion.prompt
import kotlinx.serialization.Serializable

object RespondToChallenge {
  suspend fun Promptable.respondToChallenge(
    player: Player,
    challenger: Player,
    claimedInfluence: Influence
  ): ChallengeResponse =
    prompt("RespondToChallenge", Request(claimedInfluence, challenger.playerNumber)) { (influence): Response ->
      require(influence in player.heldInfluences) { "$player does not have a $influence" }
      ChallengeResponse(influence)
    }

  @Serializable
  private data class Request(val claim: Influence, val challenger: Int)

  @Serializable
  private data class Response(val influence: Influence)
}
