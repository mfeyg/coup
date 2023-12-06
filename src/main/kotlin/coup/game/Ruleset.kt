package coup.game

import coup.game.Action.*
import coup.game.Influence.*

class Ruleset {

  enum class ActionType {
    Income, ForeignAid, Tax, Steal, Exchange, Assassinate, Coup,
  }

  sealed interface ActionBuilder {
    val type: ActionType
    val player: Player
    val targetRequired: Boolean
    var target: Player?
    fun build(): Action
  }

  private inner class ActionBuilderInstance(override val player: Player, override val type: ActionType) :
    ActionBuilder {
    override val targetRequired get() = type in setOf(ActionType.Steal, ActionType.Assassinate, ActionType.Coup)
    override var target: Player? = null
    override fun build(): Action {
      if (targetRequired != (target != null)) {
        throw IllegalStateException("Action $type " + (if (targetRequired) "requires" else "does not take") + " a target.")
      }
      return when (type) {
        ActionType.Income -> Income(player)
        ActionType.ForeignAid -> ForeignAid(player)
        ActionType.Tax -> Tax(player)
        ActionType.Steal -> Steal(player, target!!)
        ActionType.Exchange -> Exchange(player)
        ActionType.Assassinate -> Assassinate(player, target!!, cost(ActionType.Assassinate))
        ActionType.Coup -> Coup(player, target!!, cost(ActionType.Coup))
      }
    }
  }

  fun cost(actionType: ActionType) = when (actionType) {
    ActionType.Assassinate -> 3
    ActionType.Coup -> 7
    else -> 0
  }

  fun availableActions(player: Player): List<ActionBuilder> {
    return if (player.isk >= 10) listOf(ActionBuilderInstance(player, ActionType.Coup))
    else ActionType.entries.filter { action -> cost(action) <= player.isk }
      .map { ActionBuilderInstance(player, it) }
  }

  companion object {
    val Action.type
      get() = when (this) {
        is Assassinate -> ActionType.Assassinate
        is Coup -> ActionType.Coup
        is Exchange -> ActionType.Exchange
        is ForeignAid -> ActionType.ForeignAid
        is Income -> ActionType.Income
        is Steal -> ActionType.Steal
        is Tax -> ActionType.Tax
      }
  }

  private fun requiredInfluence(actionType: ActionType) = when (actionType) {
    ActionType.Tax -> Duke
    ActionType.Steal -> Captain
    ActionType.Exchange -> Ambassador
    ActionType.Assassinate -> Assassin
    else -> null
  }

  private fun blockingInfluences(actionType: ActionType) = when (actionType) {
    ActionType.ForeignAid -> setOf(Duke)
    ActionType.Steal -> setOf(Captain, Ambassador)
    ActionType.Assassinate -> setOf(Contessa)
    else -> setOf()
  }

  fun requiredInfluence(action: Action) = requiredInfluence(action.type)

  fun blockingInfluences(action: Action) = blockingInfluences(action.type)

  fun canChallenge(player: Player, action: Action): Boolean {
    return player != action.player && requiredInfluence(action) != null
  }

  fun canAttemptBlock(player: Player, action: Action): Boolean {
    if (player == action.player) return false
    return when (action) {
      is ForeignAid -> true
      is Steal -> player == action.target
      is Assassinate -> player == action.target
      else -> false
    }
  }
}