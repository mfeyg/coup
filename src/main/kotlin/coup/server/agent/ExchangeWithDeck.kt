package coup.server.agent

import coup.game.Influence
import kotlinx.serialization.Serializable

object ExchangeWithDeck {
  suspend fun PromptContext.returnCards(drawnCards: List<Influence>) = prompt {
    type = "Exchange"
    request(
      Request(drawnCards)
    )
    readResponse { (returnedInfluences): Response ->
      require(returnedInfluences.size == drawnCards.size) { "Must return ${drawnCards.size} influences." }
      val allInfluences = (player.heldInfluences + drawnCards).toMutableList()
      returnedInfluences.forEach { influence ->
        require(allInfluences.remove(influence)) { "Not enough ${influence}s." }
      }
      returnedInfluences
    }
    timeout(options.responseTimer) {
      val allInfluences = (player.heldInfluences + drawnCards).toMutableList()
      drawnCards.map { allInfluences.random().also { allInfluences.remove(it) } }
    }
  }

  @Serializable
  private data class Request(val drawnInfluences: List<Influence>)

  @Serializable
  private data class Response(val returnedInfluences: List<Influence>)
}