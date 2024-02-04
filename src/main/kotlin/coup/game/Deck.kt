package coup.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class Deck(cards: List<Influence>) {

  private val cards = MutableStateFlow(cards)

  fun shuffle() = cards.update { it.shuffled() }

  fun draw(): Influence {
    while (true) {
      val deck = this.cards.value
      val card = deck.first()
      if (this.cards.compareAndSet(deck, deck.drop(1))) {
        return card
      }
    }
  }

  fun putBack(influence: Influence) {
    cards.update { it + influence }
  }

  constructor(cards: List<Influence>, repeated: Int) : this(
    buildList { cards.forEach { card -> repeat(repeated) { add(card) } } }
  )
}