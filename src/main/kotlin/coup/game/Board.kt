package coup.game

data class Board(val deck: Deck, val players: List<Player>) {
  companion object {
    fun setUp(players: List<Player>, deck: Deck = Deck.standard()): Board {
      for (player in players) {
        repeat(2) {
          player.drawFrom(deck)
        }
        player.gainIsk(3)
      }
      deck.draw()
      return Board(deck, players)
    }
  }
}