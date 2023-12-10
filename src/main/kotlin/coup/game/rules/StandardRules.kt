package coup.game.rules

import coup.game.Board
import coup.game.Deck
import coup.game.Influence.*
import coup.game.Player
import coup.game.actions.Action
import coup.game.actions.ActionBuilder
import coup.game.actions.ActionType
import coup.game.actions.ActionType.*

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

  override fun setUpBoard(players: List<Player>): Board {

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

  override fun cost(actionType: ActionType) = when (actionType) {
    Assassinate -> 3
    Coup -> 7
    else -> 0
  }

  override fun availableActions(player: Player, board: Board) =
    if (player.isk >= 10) listOf(ActionBuilder(this, player, board, Coup))
    else actions.filter { action -> cost(action) <= player.isk }
      .map { ActionBuilder(this, player, board, it) }

  override fun requiredInfluence(actionType: ActionType) = when (actionType) {
    Tax -> Duke
    Steal -> Captain
    Exchange -> Ambassador
    Assassinate -> Assassin
    else -> null
  }

  override fun blockingInfluences(actionType: ActionType) = when (actionType) {
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