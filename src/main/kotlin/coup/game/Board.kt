package coup.game

data class Board(val deck: Deck, private val players: List<Player>) {
  val activePlayers: List<Player> get() = players.filter { it.isActive }
}