package coup.game.actions

enum class ActionType {
  Income, ForeignAid, Tax, Steal, Exchange, Assassinate, Coup;

  companion object {
    val Action.type: ActionType
      get() = when (this) {
        is Action.Assassinate -> Assassinate
        is Action.Coup -> Coup
        is Action.Exchange -> Exchange
        is Action.ForeignAid -> ForeignAid
        is Action.Income -> Income
        is Action.Steal -> Steal
        is Action.Tax -> Tax
      }
  }
}