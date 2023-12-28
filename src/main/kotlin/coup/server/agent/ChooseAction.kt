package coup.server.agent

import coup.game.Board
import coup.server.agent.ActionType.Companion.actionType
import kotlinx.serialization.Serializable

object ChooseAction {

  @Serializable
  private data class Request(val options: List<Option>)

  @Serializable
  private data class Option(
    val actionType: ActionType,
    val targets: List<Target> = emptyList(),
  )

  @Serializable
  private data class Target(
    val name: String,
    val number: Int,
  )

  @Serializable
  private data class Response(
    val actionType: ActionType,
    val target: Int? = null
  )

  suspend fun PromptContext.chooseAction(board: Board) = prompt {
    val actionsAvailable = ruleset.availableActions(player, board).associateBy { it.actionType }
    val targets = (board.activePlayers - player).associateBy { it.number }
    type = "TakeTurn"
    request(
      Request(actionsAvailable.values.map { option ->
        Option(
          option.actionType,
          if (option.targetRequired) targets.values.map { target ->
            Target(
              players[target.number].name,
              target.number
            )
          } else emptyList()
        )
      })
    )
    readResponse { (actionType, target): Response ->
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
