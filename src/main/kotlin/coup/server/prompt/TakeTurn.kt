package coup.server.prompt

import coup.game.Action
import coup.game.Player
import kotlinx.serialization.Serializable

class TakeTurn(
  options: List<Player.Agent.ActionOption>
) : Prompt<Player.Agent.ActionChoice>() {

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

  override val config = config(
    request = Request(options.map { (actionType, validTargets) ->
      Option(actionType, validTargets?.map(::Target))
    }),
    readResponse = { (actionType, target): Response ->
      Player.Agent.ActionChoice(
        actionType,
        options.find { it.actionType == actionType }?.validTargets?.find { it.playerNumber == target })
    },
  )

}
