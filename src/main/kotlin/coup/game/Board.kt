package coup.game

data class Board(val players: List<Player>, val deck: Deck) {
  companion object {
    fun setUp(players: List<Player>, deck: Deck = Deck.standard()): Board {
      for (player in players) {
        repeat(2) {
          player.draw(deck.draw())
        }
        player.gainIsk(3)
      }
      deck.draw()
      return Board(players, deck)
    }
  }
}