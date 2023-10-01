package coup.server.prompt

import coup.game.Action
import coup.game.Player
import kotlinx.serialization.Serializable

class TakeTurn(
  private val player: Player,
  private val targets: List<Player>
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
  )

  private val availableActions =
    if (player.isk >= 10) listOf(Action.Type.Coup)
    else Action.Type.entries.filter { action -> action.cost <= player.isk }

  private val request = Request(availableActions.map { action ->
    Option(
      action,
      if (action.hasTarget) {
        targets.map { Target(it.name, it.playerNumber) }
      } else null)
  })

  override val config = config(
    request = request,
    readResponse = { (actionType, target): Response ->
      Action.create(actionType, player, targets.find { it.playerNumber == target })
    },
    validate = { require { it.type in availableActions } }
  )

}
