package coup.game.rules

import coup.game.actions.Action.*
import coup.game.Influence.*
import coup.game.Player
import coup.game.actions.Action
import coup.game.actions.ActionBuilder
import coup.game.actions.ActionType

class StandardRuleset : Ruleset {

  private val allActions = listOf(
    ActionType.Income,
    ActionType.ForeignAid,
    ActionType.Tax,
    ActionType.Steal,
    ActionType.Exchange,
    ActionType.Assassinate,
    ActionType.Coup,
  )

  override fun cost(actionType: ActionType) = when (actionType) {
    ActionType.Assassinate -> 3
    ActionType.Coup -> 7
    else -> 0
  }

  override fun availableActions(player: Player): List<ActionBuilder> {
    return if (player.isk >= 10) listOf(ActionBuilder(this, player, ActionType.Coup))
    else allActions.filter { action -> cost(action) <= player.isk }
      .map { ActionBuilder(this, player, it) }
  }

  override fun requiredInfluence(actionType: ActionType) = when (actionType) {
    ActionType.Tax -> Duke
    ActionType.Steal -> Captain
    ActionType.Exchange -> Ambassador
    ActionType.Assassinate -> Assassin
    else -> null
  }

  override fun blockingInfluences(actionType: ActionType) = when (actionType) {
    ActionType.ForeignAid -> setOf(Duke)
    ActionType.Steal -> setOf(Captain, Ambassador)
    ActionType.Assassinate -> setOf(Contessa)
    else -> setOf()
  }

  override fun canChallenge(player: Player, action: Action): Boolean {
    return player != action.player && requiredInfluence(action) != null
  }

  override fun canAttemptBlock(player: Player, action: Action): Boolean {
    if (player == action.player) return false
    return when (action) {
      is ForeignAid -> true
      is Steal -> player == action.target
      is Assassinate -> player == action.target
      else -> false
    }
  }
}