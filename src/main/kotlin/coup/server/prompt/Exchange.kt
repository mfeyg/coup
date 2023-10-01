package coup.server.prompt

import coup.game.Influence
import kotlinx.serialization.Serializable

class Exchange(
  private val heldInfluences: List<Influence>,
  private val drawnInfluences: List<Influence>,
) : Prompt<List<Influence>>() {

  @Serializable
  private data class Request(val drawnInfluences: List<Influence>)

  @Serializable
  private data class Response(val returnedInfluences: List<Influence>)

  override val config = config(
    request = Request(drawnInfluences),
    readResponse = { response: Response -> response.returnedInfluences },
    validate = { returnedInfluences ->
      require { returnedInfluences.size == drawnInfluences.size }
      require { returnedInfluences isSublistOf (heldInfluences + drawnInfluences) }
    }
  )

  private infix fun List<Influence>.isSublistOf(list: List<Influence>): Boolean {
    val sizes = list.groupBy { it }
    return groupBy { it }.all { (influence, influences) ->
      influences.size <= (sizes[influence]?.size ?: return false)
    }
  }
}