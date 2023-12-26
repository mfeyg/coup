package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.PromptBuilder.Companion.prompt
import kotlinx.serialization.Serializable

object RespondToChallenge {
  fun PromptContext.respondToChallenge(claim: Influence, challenger: Player) = prompt {
    type = "RespondToChallenge"
    request(
      Request(claim, challenger.playerNumber)
    )
    readResponse { (influence): Response ->
      require(influence in player.heldInfluences) { "$player does not have a $influence" }
      influence
    }
  }

  @Serializable
  private data class Request(val claim: Influence, val challenger: Int)

  @Serializable
  private data class Response(val influence: Influence)
}
