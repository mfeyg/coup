package coup.server.prompt

import coup.game.actions.ActionBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
  Income,

  @SerialName("Foreign Aid")
  ForeignAid, Tax, Steal, Exchange, Assassinate, Coup;

  companion object {
    operator fun invoke(actionType: coup.game.actions.ActionType) = when (actionType) {
      coup.game.actions.ActionType.Income -> Income
      coup.game.actions.ActionType.ForeignAid -> ForeignAid
      coup.game.actions.ActionType.Tax -> Tax
      coup.game.actions.ActionType.Steal -> Steal
      coup.game.actions.ActionType.Exchange -> Exchange
      coup.game.actions.ActionType.Assassinate -> Assassinate
      coup.game.actions.ActionType.Coup -> Coup
    }

    val ActionBuilder.actionType
      get() = ActionType(type)
  }
}