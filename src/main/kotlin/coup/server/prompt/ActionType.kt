package coup.server.prompt

import coup.game.ActionBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
  Income,

  @SerialName("Foreign Aid")
  ForeignAid, Tax, Steal, Exchange, Assassinate, Coup;

  companion object {
    operator fun invoke(actionType: coup.game.ActionType) = when (actionType) {
      coup.game.ActionType.Income -> Income
      coup.game.ActionType.ForeignAid -> ForeignAid
      coup.game.ActionType.Tax -> Tax
      coup.game.ActionType.Steal -> Steal
      coup.game.ActionType.Exchange -> Exchange
      coup.game.ActionType.Assassinate -> Assassinate
      coup.game.ActionType.Coup -> Coup
    }

    val ActionBuilder.actionType
      get() = ActionType(type)
  }
}