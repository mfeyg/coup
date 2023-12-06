package coup.server.prompt

import coup.game.Action
import coup.game.Player
import coup.game.Ruleset
import coup.server.prompt.ActionType.Companion.actionType
import kotlinx.serialization.Serializable

class TakeTurn(
  options: List<Ruleset.ActionBuilder>,
  targets: List<Player>
) : Prompt<Action>() {

  @Serializable
  private data class Request(val options: List<Option>)

  @Serializable
  private data class Response(
    val actionType: ActionType,
    val target: Int? = null
  )

  @Serializable
  private data class Option(
    val actionType: ActionType,
    val targets: List<Target>,
  )

  @Serializable
  private data class Target(
    val name: String,
    val number: Int,
  ) {
    constructor(player: Player) : this(player.name, player.playerNumber)
  }

  override val config = config(
    request = Request(options.map { actionBuilder ->
      Option(actionBuilder.actionType, if (actionBuilder.targetRequired) targets.map(::Target) else listOf())
    }),
    readResponse = { (actionType, target): Response ->
      val action =
        options.find { it.actionType == actionType }
          ?: throw IllegalArgumentException("Action $actionType is not valid.")
      if (action.targetRequired) {
        val targetNumber = target ?: throw IllegalArgumentException("Action $actionType requires a target")
        action.target = targets.find { it.playerNumber == targetNumber }
          ?: throw IllegalArgumentException("Target $targetNumber not found")
      }
      action.build()
    },
  )

}
