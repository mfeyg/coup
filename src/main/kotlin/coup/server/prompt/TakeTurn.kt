package coup.server.prompt

import coup.game.Action
import coup.game.Player
import kotlinx.serialization.Serializable

class TakeTurn(
  private val player: Player,
  choices: List<Player.Agent.ActionChoice>
) : Prompt<Action>() {

  @Serializable
  private data class Request(val options: List<Option>)

  @Serializable
  private data class Response(
    val actionType: Action.Type,
    val target: Int? = null
  )

  @Serializable
  private data class Option(
    val actionType: Action.Type,
    val targets: List<Target>?
  )

  @Serializable
  private data class Target(
    val name: String,
    val number: Int,
  ) {
    constructor(player: Player) : this(player.name, player.playerNumber)
  }

  private val request = Request(choices.map { choice ->
    Option(
      choice.actionType,
      choice.validTargets?.map(::Target)
    )
  })

  override val config = config(
    request = request,
    readResponse = { (actionType, target): Response ->
      Action.create(
        actionType,
        player,
        choices.find { it.actionType == actionType }?.validTargets?.find { it.playerNumber == target })
    },
    validate = { action -> require { choices.any { it.actionType == action.type } } }
  )

}
