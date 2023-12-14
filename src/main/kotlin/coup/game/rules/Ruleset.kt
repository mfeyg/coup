package coup.game.rules

import coup.game.Board
import coup.game.Influence
import coup.game.Player
import coup.game.actions.Action.Type.Companion.type
import coup.game.actions.Action
import coup.game.actions.ActionBuilder

interface Ruleset {

  val maxPlayers: Int
  fun setUpBoard(players: List<Player>): Board
  fun cost(actionType: Action.Type): Int
  fun availableActions(player: Player, board: Board): List<ActionBuilder>
  fun requiredInfluence(actionType: Action.Type): Influence?
  fun blockingInfluences(actionType: Action.Type): Set<Influence>
  fun canChallenge(player: Player, action: Action): Boolean
  fun canAttemptBlock(player: Player, action: Action): Boolean

  fun requiredInfluence(action: Action) = requiredInfluence(action.type)
  fun blockingInfluences(action: Action) = blockingInfluences(action.type)

}