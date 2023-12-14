package coup.game

class Turn(private val players: List<Player>) {
  val currentPlayer = players.first()

  fun next(): Turn {
    val players = players.toMutableList()
    players.add(players.removeFirst())
    while (!players.first().isActive) {
      players.removeFirst()
    }
    return Turn(players)
  }
}