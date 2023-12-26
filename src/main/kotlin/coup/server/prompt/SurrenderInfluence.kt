package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.PromptBuilder.Companion.prompt
import kotlinx.serialization.Serializable

class SurrenderInfluence(private val player: Player) {
  @Serializable
  private data class Response(val influence: Influence)

  fun surrenderInfluence() = prompt {
    type = "SurrenderInfluence"
    readResponse { (influence): Response ->
      require(influence in player.heldInfluences)
      influence
    }
  }
}
