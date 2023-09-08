package coup.game

sealed interface GameEvent {
  data class TurnStarted(val player: Player) : GameEvent
  data class GameOver(val winner: Player) : GameEvent
  data class ActionAttempted(val action: Action) : GameEvent
  data class ActionPerformed(val action: Action) : GameEvent
  data class ActionChallenged(val action: Action, val challenger: Player) : GameEvent
  data class BlockAttempted(val action: Action, val blocker: Player, val influence: Influence) : GameEvent
  data class ActionBlocked(val action: Action, val blocker: Player, val influence: Influence) : GameEvent
  data class BlockChallenged(
    val action: Action,
    val blocker: Player,
    val influence: Influence,
    val challenger: Player
  ) : GameEvent
  data class InfluenceRevealed(val player: Player, val influence: Influence) : GameEvent
  data class InfluenceSurrendered(val player: Player, val influence: Influence) : GameEvent
}
