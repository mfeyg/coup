package coup.game

import kotlinx.serialization.Serializable

sealed interface GameEvent {
  @Serializable
  data class TurnStarted(val player: Int) : GameEvent {
    constructor(player: Player) : this(player.playerNumber)
  }

  @Serializable
  data class GameOver(val winner: Int) : GameEvent {
    constructor(winner: Player) : this(winner.playerNumber)
  }

  @Serializable
  data class ActionAttempted(val player: Int, val actionType: Action.Type, val target: Int?) : GameEvent {
    constructor(action: Action) : this(action.player.playerNumber, action.type, action.target?.playerNumber)
  }

  @Serializable
  data class ActionPerformed(val player: Int, val actionType: Action.Type, val target: Int?) : GameEvent {
    constructor(action: Action) : this(action.player.playerNumber, action.type, action.target?.playerNumber)
  }

  @Serializable
  data class ActionChallenged(
    val player: Int,
    val actionType: Action.Type,
    val neededInfluence: Influence,
    val target: Int?,
    val challenger: Int
  ) :
    GameEvent {
    constructor(action: Action, challenger: Player) : this(
      action.player.playerNumber,
      action.type,
      action.type.neededInfluence!!,
      action.target?.playerNumber,
      challenger.playerNumber
    )
  }

  @Serializable
  data class BlockAttempted(
    val player: Int,
    val actionType: Action.Type,
    val target: Int?,
    val blocker: Int,
    val influence: Influence
  ) : GameEvent {
    constructor(action: Action, blocker: Player, influence: Influence) : this(
      action.player.playerNumber, action.type, action.target?.playerNumber, blocker.playerNumber, influence
    )
  }

  @Serializable
  data class ActionBlocked(
    val player: Int,
    val actionType: Action.Type,
    val target: Int?,
    val blocker: Int,
    val influence: Influence
  ) : GameEvent {
    constructor(action: Action, blocker: Player, influence: Influence) : this(
      action.player.playerNumber, action.type, action.target?.playerNumber, blocker.playerNumber, influence
    )
  }

  @Serializable
  data class BlockChallenged(
    val player: Int,
    val actionType: Action.Type,
    val target: Int?,
    val blocker: Int,
    val influence: Influence,
    val challenger: Int,
  ) : GameEvent {
    constructor(action: Action, blocker: Player, influence: Influence, challenger: Player) : this(
      action.player.playerNumber,
      action.type,
      action.target?.playerNumber,
      blocker.playerNumber,
      influence,
      challenger.playerNumber
    )
  }

  @Serializable
  data class InfluenceRevealed(val player: Int, val influence: Influence) : GameEvent {
    constructor(player: Player, influence: Influence) : this(player.playerNumber, influence)
  }

  @Serializable
  data class InfluenceSurrendered(val player: Int, val influence: Influence) : GameEvent {
    constructor(player: Player, influence: Influence) : this(player.playerNumber, influence)
  }
}
