package coup.server.prompt

import coup.game.Influence
import kotlinx.serialization.Serializable

class SurrenderInfluence : Prompt<Influence>() {
  @Serializable
  data class Response(val influence: Influence)

  override fun prompt() = sendAndReceive { response: Response -> response.influence }
}