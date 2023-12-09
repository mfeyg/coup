package coup.game.rules

import coup.game.Influence
import coup.game.Player
import coup.game.action.ActionType.Companion.type
import coup.game.action.Action
import coup.game.action.ActionBuilder
import coup.game.action.ActionType

interface Ruleset {

  fun cost(actionType: ActionType): Int
  fun availableActions(player: Player): List<ActionBuilder>
  fun requiredInfluence(actionType: ActionType): Influence?
  fun blockingInfluences(actionType: ActionType): Set<Influence>
  fun canChallenge(player: Player, action: Action): Boolean
  fun canAttemptBlock(player: Player, action: Action): Boolean

  fun requiredInfluence(action: Action) = requiredInfluence(action.type)
  fun blockingInfluences(action: Action) = blockingInfluences(action.type)
}