package coup.game

import coup.game.Influence.*
import kotlin.test.Test
import kotlin.test.assertContentEquals

class DeckTest {
  @Test
  fun testConstruction() {
    val deck = Deck(listOf(Contessa, Assassin, Ambassador), 3)
    val cards = mutableListOf<Influence>()
    repeat(9) { cards += deck.draw() }

    assertContentEquals(
      cards,
      listOf(
        Contessa, Contessa, Contessa,
        Assassin, Assassin, Assassin,
        Ambassador, Ambassador, Ambassador
      )
    )
  }
}