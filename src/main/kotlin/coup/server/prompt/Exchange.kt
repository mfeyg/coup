package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.Prompt
import coup.server.Prompt.Companion.prompt
import kotlinx.serialization.Serializable

object Exchange {
  suspend fun Prompt.exchange(player: Player, drawnInfluences: List<Influence>) =
    prompt("Exchange", Request(drawnInfluences)) { (returnedInfluences): Response ->
      require(returnedInfluences.size == drawnInfluences.size) { "Must return ${drawnInfluences.size} influences" }
      require(returnedInfluences sublist drawnInfluences + player.heldInfluences)
      returnedInfluences
    }

  @Serializable
  private data class Request(val drawnInfluences: List<Influence>)

  @Serializable
  private data class Response(val returnedInfluences: List<Influence>)

  private infix fun <T> List<T>.sublist(list: List<T>): Boolean {
    val sizes = list.groupBy { it }
    return groupBy { it }.all { (item, items) ->
      items.size <= (sizes[item]?.size ?: return false)
    }
  }
}