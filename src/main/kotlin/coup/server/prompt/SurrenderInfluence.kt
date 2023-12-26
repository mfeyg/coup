package coup.server.prompt

import coup.game.Influence
import kotlinx.serialization.Serializable

object SurrenderInfluence {
  @Serializable
  private data class Response(val influence: Influence)

  suspend fun PromptContext.surrenderInfluence() = prompt {
    type = "SurrenderInfluence"
    readResponse { (influence): Response ->
      require(influence in player.heldInfluences)
      influence
    }
  }
}
