package coup.server.prompt

import coup.game.Influence
import kotlinx.serialization.Serializable

class SurrenderInfluence(private val heldInfluences: List<Influence>) : Prompt<Influence>() {

  @Serializable
  private data class Response(val influence: Influence)

  override val config = config(
    readResponse = { response: Response -> response.influence },
    validate = { require { it in heldInfluences } }
  )

}