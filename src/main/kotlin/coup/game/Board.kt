package coup.game

import kotlinx.coroutines.flow.combine

data class Board(val deck: Deck, val players: List<Player>) {
  /** Players still in the game. */
  val activePlayers: List<Player> get() = players.filter { it.isActive }

  /** A flow that is triggered whenever the board changes. */
  val updates = combine(players.map { it.updates }) {}
}