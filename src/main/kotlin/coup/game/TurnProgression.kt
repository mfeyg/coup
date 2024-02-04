package coup.game

class TurnProgression(private val players: List<Player>) {
  val currentPlayer = players.first()

  fun next(): TurnProgression {
    val players = players.toMutableList()
    players.add(players.removeFirst())
    while (!players.first().isActive) {
      players.removeFirst()
    }
    return TurnProgression(players)
  }
}