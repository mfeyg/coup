package coup.server.agent

import coup.game.Influence
import coup.game.Player
import coup.server.dto.PlayerData
import coup.server.dto.PlayerData.Companion.dto
import kotlinx.serialization.Serializable

object RespondToChallenge {
  suspend fun PromptContext.respondToChallenge(claim: Influence, challenger: Player) = prompt {
    type = "RespondToChallenge"
    request(
      Request(claim, challenger.dto())
    )
    readResponse { (influence): Response ->
      require(influence in player.heldInfluences) { "$player does not have a $influence" }
      influence
    }
    timeout(options.responseTimer) {
      if (claim in player.heldInfluences) claim else player.heldInfluences.random()
    }
  }

  @Serializable
  private data class Request(val claim: Influence, val challenger: PlayerData)

  @Serializable
  private data class Response(val influence: Influence)
}
