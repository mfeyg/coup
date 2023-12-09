package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.Prompt
import coup.server.Prompt.Companion.prompt
import kotlinx.serialization.Serializable

object Exchange {
  suspend fun Prompt.exchange(player: Player, drawnInfluences: List<Influence>) =
    prompt("Exchange", Request(drawnInfluences)) { (returnedInfluences): Response ->
      require(returnedInfluences.size == drawnInfluences.size) { "Must return ${drawnInfluences.size} influences." }
      val allInfluences = (player.heldInfluences + drawnInfluences).toMutableList()
      returnedInfluences.forEach { influence ->
        require(allInfluences.remove(influence)) { "Not enough ${influence}s." }
      }
      returnedInfluences
    }

  @Serializable
  private data class Request(val drawnInfluences: List<Influence>)

  @Serializable
  private data class Response(val returnedInfluences: List<Influence>)
}