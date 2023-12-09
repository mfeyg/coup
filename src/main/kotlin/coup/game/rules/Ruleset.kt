package coup.game.rules

import coup.game.Board
import coup.game.Influence
import coup.game.Player
import coup.game.actions.ActionType.Companion.type
import coup.game.actions.Action
import coup.game.actions.ActionBuilder
import coup.game.actions.ActionType

interface Ruleset {

  fun setUpBoard(players: List<Player>): Board
  fun cost(actionType: ActionType): Int
  fun availableActions(player: Player): List<ActionBuilder>
  fun requiredInfluence(actionType: ActionType): Influence?
  fun blockingInfluences(actionType: ActionType): Set<Influence>
  fun canChallenge(player: Player, action: Action): Boolean
  fun canAttemptBlock(player: Player, action: Action): Boolean

  fun requiredInfluence(action: Action) = requiredInfluence(action.type)
  fun blockingInfluences(action: Action) = blockingInfluences(action.type)

}