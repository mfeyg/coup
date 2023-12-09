package coup.game.rules

import coup.game.Influence.*
import coup.game.Player
import coup.game.actions.Action
import coup.game.actions.ActionBuilder
import coup.game.actions.ActionType
import coup.game.actions.ActionType.*

class StandardRuleset : Ruleset {

  private val allActions = listOf(
    Income,
    ForeignAid,
    Tax,
    Steal,
    Exchange,
    Assassinate,
    Coup,
  )

  override fun cost(actionType: ActionType) = when (actionType) {
    Assassinate -> 3
    Coup -> 7
    else -> 0
  }

  override fun availableActions(player: Player) =
    if (player.isk >= 10) listOf(ActionBuilder(this, player, Coup))
    else allActions.filter { action -> cost(action) <= player.isk }
      .map { ActionBuilder(this, player, it) }

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