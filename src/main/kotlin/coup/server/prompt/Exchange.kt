package coup.server.prompt

import coup.game.Influence
import kotlinx.serialization.Serializable

class Exchange(drawnInfluences: List<Influence>) : Prompt<List<Influence>>() {

  @Serializable
  data class Request(val drawnInfluences: List<Influence>)

  private val request = Request(drawnInfluences)

  @Serializable
  data class Response(val returnedInfluences: List<Influence>)

  override fun prompt() = sendAndReceive(request) { response: Response -> response.returnedInfluences }
}