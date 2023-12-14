package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.prompt.Promptable.Companion.prompt
import kotlinx.serialization.Serializable

class SurrenderInfluence(private val player: Player, private val promptable: Promptable){
  @Serializable
  private data class Response(val influence: Influence)

  suspend fun surrenderInfluence(): Influence =
    promptable.prompt("SurrenderInfluence") { (influence): Response ->
      require(influence in player.heldInfluences)
      influence
    }
}
