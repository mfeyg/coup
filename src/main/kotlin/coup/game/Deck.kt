package coup.game

class Deck(private var cards: List<Influence>) {
  fun shuffle() {
    cards = cards.shuffled()
  }

  fun draw(): Influence {
    val influence = cards[0]
    cards = cards.drop(1)
    return influence
  }

  fun putBack(influence: Influence) {
    cards = cards + listOf(influence)
  }

  constructor(cards: List<Influence>, repeat: Int) : this(
    sequence { cards.forEach { card -> repeat(repeat) { yield(card) } } }.toList()
  )
}