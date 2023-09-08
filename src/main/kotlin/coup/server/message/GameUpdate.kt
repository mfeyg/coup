package coup.server.message

import coup.game.Influence
import kotlinx.serialization.Serializable

@Serializable
data class GameUpdate(val game: GameModel): Message {
  @Serializable
  data class GameModel(
    val playerName: String,
    val playerNumber: Int,
    val playerIsk: Int,
    val playerTurn: Boolean,
    val heldInfluences: List<Influence>,
    val revealedInfluences: List<Influence>,
    val opponents: List<Opponent>,
    val currentTurn: Int,
  ) {
    @Serializable
    data class Opponent(
      val name: String,
      val number: Int,
      val isk: Int,
      val opponentTurn: Boolean,
      val heldInfluences: Int,
      val revealedInfluences: List<Influence>,
    )
  }
}