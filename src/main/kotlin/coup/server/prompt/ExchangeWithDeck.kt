package coup.server.prompt

import coup.game.Influence
import coup.game.Player
import coup.server.prompt.Promptable.Companion.prompt
import kotlinx.serialization.Serializable

class ExchangeWithDeck(private val player: Player, private val promptable: Promptable) {
  suspend fun returnCards(drawnCards: List<Influence>): List<Influence> =
    promptable.prompt("Exchange", Request(drawnCards)) { (returnedInfluences): Response ->
      require(returnedInfluences.size == drawnCards.size) { "Must return ${drawnCards.size} influences." }
      val allInfluences = (player.heldInfluences + drawnCards).toMutableList()
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