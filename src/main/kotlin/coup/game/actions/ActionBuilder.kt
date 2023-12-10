package coup.game.actions

import coup.game.Board
import coup.game.Player
import coup.game.actions.Action.Type.*
import coup.game.rules.Ruleset

class ActionBuilder(
  private val ruleset: Ruleset,
  val player: Player,
  private val board: Board,
  val type: Action.Type
) {

  var target: Player? = null

  val targetRequired
    get() = type in setOf(Steal, Assassinate, Coup)

  fun build(): Action {
    if (targetRequired != (target != null)) {
      throw IllegalStateException("Action $type " + (if (targetRequired) "requires" else "does not take") + " a target.")
    }
    return when (type) {
      Income -> Action.Income(player)
      ForeignAid -> Action.ForeignAid(player)
      Tax -> Action.Tax(player)
      Steal -> Action.Steal(player, target!!)
      Exchange -> Action.Exchange(player, board.deck)
      Assassinate -> Action.Assassinate(player, target!!, ruleset.cost(Assassinate))
      Coup -> Action.Coup(player, target!!, ruleset.cost(Coup))
    }
  }
}