package coup.server.prompt

import coup.game.Board
import coup.game.Player
import coup.game.actions.Action
import coup.game.actions.ActionBuilder
import coup.game.rules.Ruleset
import coup.server.prompt.ActionType.Companion.actionType
import coup.server.prompt.Promptable.Companion.prompt
import kotlinx.serialization.Serializable

class ChooseAction(
  private val player: Player,
  private val promptable: Promptable,
  private val ruleset: Ruleset
)  {

  @Serializable
  private data class Request(val options: List<Option>)

  @Serializable
  private data class Option(
    val actionType: ActionType,
    val targets: List<Target>,
  ) {
    constructor(actionBuilder: ActionBuilder, targets: Iterable<Player>) : this(
      actionType = actionBuilder.actionType,
      targets = if (actionBuilder.targetRequired) targets.map(::Target) else listOf()
    )
  }

  @Serializable
  private data class Target(
    val name: String,
    val number: Int,
  ) {
    constructor(player: Player) : this(player.name, player.playerNumber)
  }

  @Serializable
  private data class Response(
    val actionType: ActionType,
    val target: Int? = null
  )

  suspend fun chooseAction(board: Board): Action {
    val actionsAvailable = ruleset.availableActions(player, board).associateBy { it.actionType }
    val targets = (board.activePlayers - player).associateBy { it.playerNumber }
    return promptable.prompt(
      "TakeTurn",
      Request(actionsAvailable.values.map { Option(it, targets.values) })
    ) { (actionType, target): Response ->
      val action = actionsAvailable[actionType] ?: throw IllegalArgumentException("$actionType is not a valid action.")
      if (action.targetRequired) {
        val targetNumber = target ?: throw IllegalArgumentException("Action $actionType requires a target")
        action.target = targets[targetNumber]
          ?: throw IllegalArgumentException("Invalid target $targetNumber")
      }
      action.build()
    }
  }
}
