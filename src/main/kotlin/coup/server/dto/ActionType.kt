package coup.server.dto

import coup.game.actions.Action
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
  @SerialName("Foreign Aid")
  ForeignAid,
  Income, Tax, Steal, Exchange, Assassinate, Coup;

  companion object {
    fun Action.Type.dto() = when (this) {
      Action.Type.Income -> Income
      Action.Type.ForeignAid -> ForeignAid
      Action.Type.Tax -> Tax
      Action.Type.Steal -> Steal
      Action.Type.Exchange -> Exchange
      Action.Type.Assassinate -> Assassinate
      Action.Type.Coup -> Coup
    }
  }
}