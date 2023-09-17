package coup.server.prompt

import coup.game.Influence
import kotlinx.serialization.Serializable

class SurrenderInfluence(private val heldInfluences: List<Influence>) : Prompt<Influence>() {
  @Serializable
  data class Response(val influence: Influence)

  override fun prompt() = sendAndReceive { response: Response -> response.influence }

  override fun validate(response: Influence) {
    require { heldInfluences.contains(response) }
  }
}