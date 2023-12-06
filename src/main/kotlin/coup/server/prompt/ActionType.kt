package coup.server.prompt

import coup.game.Ruleset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
  Income,

  @SerialName("Foreign Aid")
  ForeignAid, Tax, Steal, Exchange, Assassinate, Coup;

  companion object {
    operator fun invoke(actionType: Ruleset.ActionType) = when (actionType) {
      Ruleset.ActionType.Income -> Income
      Ruleset.ActionType.ForeignAid -> ForeignAid
      Ruleset.ActionType.Tax -> Tax
      Ruleset.ActionType.Steal -> Steal
      Ruleset.ActionType.Exchange -> Exchange
      Ruleset.ActionType.Assassinate -> Assassinate
      Ruleset.ActionType.Coup -> Coup
    }

    val Ruleset.ActionBuilder.actionType
      get() = ActionType(type)
  }
}