package coup.server.prompt

import coup.game.Action
import coup.game.Player
import kotlinx.serialization.Serializable

class TakeTurn(private val player: Player, private val targets: List<Player>) : Prompt<Action>() {

  @Serializable
  data class Request(val options: List<Option>)

  @Serializable
  data class Option(
    val actionType: Action.Type,
    val targets: List<Target>?
  )

  @Serializable
  data class Target(
    val name: String,
    val number: Int,
  )

  @Serializable
  data class Response(val actionType: Action.Type, val target: Int? = null)

  private val availableActions get() = Action.Type.entries.filter { action -> action.cost <= player.isk }

  private val request: Request
    get() = Request(availableActions.map { action ->
      Option(
        action,
        if (action.hasTarget) targets.map { Target(it.name, it.playerNumber) } else null)
    })

  override fun prompt() = sendAndReceive(request) { (actionType, target): Response ->
    Action.create(actionType, player, targets.find { it.playerNumber == target })
  }

  override fun validate(response: Action) {
    require { response.type in availableActions }
  }
}
