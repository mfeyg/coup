package coup.game.rules

import coup.game.Board
import coup.game.Deck
import coup.game.Influence.*
import coup.game.Player
import coup.game.actions.Action
import coup.game.actions.Action.Type.*
import coup.game.actions.ActionBuilder

class StandardRules : Ruleset {

  private val influences = listOf(
    Duke,
    Captain,
    Assassin,
    Ambassador,
    Contessa,
  )

  private val actions = listOf(
    Income,
    ForeignAid,
    Tax,
    Steal,
    Exchange,
    Assassinate,
    Coup,
  )

  override val maxPlayers = 6

  override fun setUpBoard(players: List<Player>): Board {

    if (players.size > maxPlayers) {
      throw IllegalArgumentException("Too many players!")
    }

    val deck = Deck(influences, 3)
    deck.shuffle()

    for (player in players) {
      repeat(2) {
        player.drawFrom(deck)
      }
      player.gainIsk(3)
    }

    // burn a card
    deck.draw()

    return Board(deck, players)
  }

  override fun cost(actionType: Action.Type) = when (actionType) {
    Assassinate -> 3
    Coup -> 7
    else -> 0
  }

  override fun availableActions(player: Player, board: Board): List<ActionBuilder> {
    val possibleActions =
      if (player.isk >= 10) listOf(Coup)
      else actions.filter { action -> cost(action) <= player.isk }
    return possibleActions.map { ActionBuilder(this, player, board, it) }
  }

  override fun requiredInfluence(actionType: Action.Type) = when (actionType) {
    Tax -> Duke
    Steal -> Captain
    Exchange -> Ambassador
    Assassinate -> Assassin
    else -> null
  }

  override fun blockingInfluences(actionType: Action.Type) = when (actionType) {
    ForeignAid -> setOf(Duke)
    Steal -> setOf(Captain, Ambassador)
    Assassinate -> setOf(Contessa)
    else -> setOf()
  }

  override fun canChallenge(player: Player, action: Action) =
    player != action.player && requiredInfluence(action) != null

  override fun canAttemptBlock(player: Player, action: Action): Boolean {
    if (player == action.player) return false
    return when (action) {
      is Action.ForeignAid -> true
      is Action.Steal -> player == action.target
      is Action.Assassinate -> player == action.target
      else -> false
    }
  }
}