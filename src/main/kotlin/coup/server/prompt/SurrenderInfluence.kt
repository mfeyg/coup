package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.Prompt
import coup.server.Prompt.Companion.prompt
import kotlinx.serialization.Serializable

object SurrenderInfluence {
  @Serializable
  private data class Response(val influence: Influence)

  suspend fun Prompt.surrenderInfluence(player: Player): Influence =
    prompt("SurrenderInfluence") { (influence): Response ->
      require(influence in player.heldInfluences)
      influence
    }
}
