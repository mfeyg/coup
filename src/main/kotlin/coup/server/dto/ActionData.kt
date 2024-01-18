package coup.server.dto

import coup.game.Influence
import coup.game.Player
import coup.game.actions.Action
import coup.game.actions.Action.Type.Companion.type
import coup.game.rules.Ruleset
import coup.server.dto.ActionType.Companion.dto
import coup.server.dto.PlayerData.Companion.dto
import kotlinx.serialization.Serializable

@Serializable
data class ActionData(
  val type: ActionType,
  val player: PlayerData,
  val target: PlayerData?,
  val canBeChallenged: Boolean,
  val canBeBlocked: Boolean,
  val blockingInfluences: Set<Influence>,
  val claimedInfluence: Influence?,
) {
  companion object {
    fun Action.dto(player: Player, ruleset: Ruleset) = ActionData(
      type = this.type.dto(),
      player = this.player.dto(),
      target = this.target?.dto(),
      canBeChallenged = ruleset.canChallenge(player, this),
      canBeBlocked = ruleset.canAttemptBlock(player, this),
      blockingInfluences = ruleset.blockingInfluences(this),
      claimedInfluence = ruleset.requiredInfluence(this),
    )
  }
}