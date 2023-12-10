package coup.game.actions

import coup.game.Board
import coup.game.Player
import coup.game.rules.Ruleset

class ActionBuilder(
  private val ruleset: Ruleset,
  val player: Player,
  private val board: Board,
  val type: ActionType
) {

  var target: Player? = null

  val targetRequired
    get() = type in setOf(
      ActionType.Steal,
      ActionType.Assassinate,
      ActionType.Coup,
    )

  fun build(): Action {
    if (targetRequired != (target != null)) {
      throw IllegalStateException("Action $type " + (if (targetRequired) "requires" else "does not take") + " a target.")
    }
    return when (type) {
      ActionType.Income -> Action.Income(player)
      ActionType.ForeignAid -> Action.ForeignAid(player)
      ActionType.Tax -> Action.Tax(player)
      ActionType.Steal -> Action.Steal(player, target!!)
      ActionType.Exchange -> Action.Exchange(player, board.deck)
      ActionType.Assassinate -> Action.Assassinate(
        player,
        target!!,
        ruleset.cost(ActionType.Assassinate)
      )

      ActionType.Coup -> Action.Coup(player, target!!, ruleset.cost(ActionType.Coup))
    }
  }
}