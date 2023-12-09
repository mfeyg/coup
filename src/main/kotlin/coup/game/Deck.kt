package coup.game

class Deck(cards: Iterable<Influence>) {

  private val cards = cards.toMutableList()

  fun shuffle() = cards.shuffle()

  fun draw() = cards.removeFirst()

  fun putBack(influence: Influence) {
    cards.add(influence)
  }

  constructor(cards: List<Influence>, repeat: Int) : this(
    sequence { cards.forEach { card -> repeat(repeat) { yield(card) } } }.asIterable()
  )
}