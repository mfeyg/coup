package coup.server

import coup.game.Influence
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
  val player: Player? = null,
  val players: List<Opponent>,
  val currentTurn: Int? = null,
  val winner: Int? = null,
) {
  @Serializable
  data class Player(
    val name: String,
    val color: String,
    val number: Int,
    val isk: Int,
    val heldInfluences: List<Influence>,
    val revealedInfluences: List<Influence>,
  ) {
    constructor(player: coup.game.Player, color: String) : this(
      name = player.name,
      color = color,
      number = player.playerNumber,
      isk = player.isk,
      heldInfluences = player.heldInfluences,
      revealedInfluences = player.revealedInfluences,
    )
  }

  @Serializable
  data class Opponent(
    val name: String,
    val color: String,
    val number: Int,
    val isk: Int,
    val heldInfluences: Int,
    val revealedInfluences: List<Influence>,
  ) {
    constructor(player: coup.game.Player, color: String) : this(
      name = player.name,
      color = color,
      number = player.playerNumber,
      isk = player.isk,
      heldInfluences = player.heldInfluences.size,
      revealedInfluences = player.revealedInfluences,
    )
  }

  companion object {
    operator fun invoke(
      players: List<coup.game.Player>,
      playerColor: (coup.game.Player) -> String,
      thisPlayer: coup.game.Player?,
      currentPlayer: coup.game.Player?,
      winner: coup.game.Player?
    ) = GameState(
      players = players.map { Opponent(it, playerColor(it)) },
      player = thisPlayer?.let { Player(it, playerColor(it)) },
      currentTurn = currentPlayer?.playerNumber.takeIf { winner == null },
      winner = winner?.playerNumber,
    )
  }
}