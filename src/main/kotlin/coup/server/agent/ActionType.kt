package coup.server.agent

import coup.game.actions.Action
import coup.game.actions.ActionBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
  Income,

  @SerialName("Foreign Aid")
  ForeignAid, Tax, Steal, Exchange, Assassinate, Coup;

  companion object {
    operator fun invoke(actionType: Action.Type) = when (actionType) {
      Action.Type.Income -> Income
      Action.Type.ForeignAid -> ForeignAid
      Action.Type.Tax -> Tax
      Action.Type.Steal -> Steal
      Action.Type.Exchange -> Exchange
      Action.Type.Assassinate -> Assassinate
      Action.Type.Coup -> Coup
    }

    val ActionBuilder.actionType
      get() = ActionType(type)
  }
}