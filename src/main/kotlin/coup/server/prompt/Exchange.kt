package coup.server.prompt

import coup.game.Influence
import kotlinx.serialization.Serializable

class Exchange(private val heldInfluences: List<Influence>, private val drawnInfluences: List<Influence>) :
  Prompt<List<Influence>>() {

  @Serializable
  data class Request(val drawnInfluences: List<Influence>)

  private val request = Request(drawnInfluences)

  @Serializable
  data class Response(val returnedInfluences: List<Influence>)

  override fun prompt() = sendAndReceive(request) { response: Response -> response.returnedInfluences }

  override fun validate(response: List<Influence>) {
    val returnedInfluences = response
    require { returnedInfluences.size == drawnInfluences.size }
    require { returnedInfluences isSublistOf (heldInfluences + drawnInfluences) }
  }

  private infix fun List<Influence>.isSublistOf(list: List<Influence>): Boolean {
    val sizes = list.groupBy { it }
    return groupBy { it }.all { (influence, influences) ->
      influences.size <= (sizes[influence]?.size ?: return false)
    }
  }
}