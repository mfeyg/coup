package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.Session
import kotlinx.serialization.Serializable

class RespondToChallenge(private val player: Player, private val session: Session<*, *>) {
  suspend fun respondToChallenge(
    claim: Influence,
    challenger: Player,
  ): Influence = session.prompt {
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
