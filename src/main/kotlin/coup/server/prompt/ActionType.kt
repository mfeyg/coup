package coup.server.prompt

import coup.game.action.ActionBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
  Income,

  @SerialName("Foreign Aid")
  ForeignAid, Tax, Steal, Exchange, Assassinate, Coup;

  companion object {
    operator fun invoke(actionType: coup.game.action.ActionType) = when (actionType) {
      coup.game.action.ActionType.Income -> Income
      coup.game.action.ActionType.ForeignAid -> ForeignAid
      coup.game.action.ActionType.Tax -> Tax
      coup.game.action.ActionType.Steal -> Steal
      coup.game.action.ActionType.Exchange -> Exchange
      coup.game.action.ActionType.Assassinate -> Assassinate
      coup.game.action.ActionType.Coup -> Coup
    }

    val ActionBuilder.actionType
      get() = ActionType(type)
  }
}