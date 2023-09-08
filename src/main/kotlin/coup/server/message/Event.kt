package coup.server.message

import coup.game.Action
import coup.game.GameEvent
import coup.game.Influence
import coup.game.Player
import kotlinx.serialization.Serializable
import coup.game.Action.Type as ActionType

@Serializable
data class Event(val description: String) : Message {
  constructor(event: GameEvent) : this(event.description)

  companion object {
    private val GameEvent.description: String
      get() = when (this) {
        is GameEvent.ActionAttempted -> "${action.player} ${attempted(action.type, action.target)}."
        is GameEvent.ActionChallenged -> "$challenger challenged ${action.player}'s claim of influence with ${the(action.type.neededInfluence!!)}."
        is GameEvent.ActionPerformed -> "${action.player} ${performed(action.type, action.target)}."
        is GameEvent.BlockAttempted -> "$blocker attempted to block ${action.player}'s ${attemptOf(action.type)} on behalf of ${the(influence)}."
        is GameEvent.BlockChallenged -> "$challenger challenged ${blocker}'s claim of influence with ${the(influence)}."
        is GameEvent.ActionBlocked -> "$blocker blocked ${action.player}'s ${attemptOf(action.type)} on behalf of ${the(influence)}."
        is GameEvent.GameOver -> "$winner won the round!"
        is GameEvent.InfluenceRevealed -> "$player revealed ${the(influence)}."
        is GameEvent.InfluenceSurrendered -> "$player surrendered their influence with ${the(influence)}."
        is GameEvent.TurnStarted -> "${player}'s turn has started."
      }

    private fun performed(actionType: ActionType, target: Player?) = when (actionType) {
      Action.Type.Income -> "took income"
      Action.Type.ForeignAid -> "accepted foreign aid"
      Action.Type.Tax -> "collected taxes"
      Action.Type.Steal -> "stole from $target"
      Action.Type.Exchange -> "exchanged with the court"
      Action.Type.Assassinate -> "assassinated $target"
      Action.Type.Coup -> "performed a coup against $target"
    }

    private fun attempted(actionType: ActionType, target: Player?) = when (actionType) {
      Action.Type.ForeignAid -> "invoked foreign aid"
      Action.Type.Tax -> "tried to collect taxes"
      Action.Type.Steal -> "tried to steal from $target"
      Action.Type.Exchange -> "tried to exchange with the court"
      Action.Type.Assassinate -> "tried to assassinate $target"
      Action.Type.Income -> "attempted to collect income"
      Action.Type.Coup -> "attempted a coup against $target"
    }

    private fun attemptOf(actionType: ActionType) = when (actionType) {
      Action.Type.ForeignAid -> "foreign aid"
      Action.Type.Tax -> "taxes"
      Action.Type.Steal -> "theft"
      Action.Type.Exchange -> "exchange"
      Action.Type.Assassinate -> "assassination"
      Action.Type.Income -> "income"
      Action.Type.Coup -> "coup"
    }

    private fun the(influence: Influence) = when (influence) {
      Influence.Duke -> "the Duke"
      Influence.Captain -> "the Captain"
      Influence.Assassin -> "the Assassin"
      Influence.Ambassador -> "the Ambassador"
      Influence.Contessa -> "the Contessa"
    }
  }
}