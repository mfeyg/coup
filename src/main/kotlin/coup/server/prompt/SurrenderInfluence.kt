package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.Session
import kotlinx.serialization.Serializable

class SurrenderInfluence(private val player: Player, private val session: Session<*, *>) {
  @Serializable
  private data class Response(val influence: Influence)

  suspend fun surrenderInfluence(): Influence = session.prompt {
    type = "SurrenderInfluence"
    readResponse { (influence): Response ->
      require(influence in player.heldInfluences)
      influence
    }
  }
}
