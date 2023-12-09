package coup.game

import coup.game.ActionType.Companion.type

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