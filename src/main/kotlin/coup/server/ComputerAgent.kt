package coup.server

import coup.game.*
import coup.game.actions.Action
import coup.game.rules.Ruleset
import kotlin.random.Random

class ComputerAgent(private val ruleset: Ruleset, private val player: Player) : Agent {
  private val random = Random

  override suspend fun chooseAction(board: Board): Action {
    var actions = ruleset.availableActions(player, board)
    val cheat = random.nextBoolean()
    if (!cheat) actions =
      actions.filter { action -> ruleset.requiredInfluence(action.type)?.let { it in player.heldInfluences } ?: true }
    val action = actions.random()
    if (action.targetRequired)
      action.target = (board.activePlayers - player).random()
    return action.build()
  }

  override suspend fun chooseCardsToReturn(drawnCards: List<Influence>): List<Influence> {
    val cards = (drawnCards + player.heldInfluences).toMutableList()
    return drawnCards.map { cards.random().also { cards.remove(it) } }
  }

  override suspend fun chooseReaction(action: Action): Reaction {
    val cheat = random.nextBoolean()
    if (random.nextBoolean()) return Reaction.Allow
    if (ruleset.canAttemptBlock(player, action)
      && (cheat || player.heldInfluences.intersect(ruleset.blockingInfluences(action)).isNotEmpty())
      && random.nextBoolean()
    ) return Reaction.Block(
      player,
      player.heldInfluences.intersect(ruleset.blockingInfluences(action)).randomOrNull()
        ?: ruleset.blockingInfluences(action).random()
    )
    if (ruleset.canChallenge(player, action) && random.nextBoolean() && random.nextBoolean()) {
      return Reaction.Challenge(player)
    }
    return Reaction.Allow
  }

  override suspend fun chooseInfluenceToReveal(claimedInfluence: Influence, challenger: Player): Influence {
    return if (claimedInfluence in player.heldInfluences) {
      claimedInfluence
    } else {
      player.heldInfluences.random()
    }
  }

  override suspend fun chooseWhetherToChallenge(block: Reaction.Block): Boolean {
    return random.nextBoolean() && random.nextBoolean() && random.nextBoolean()
  }

  override suspend fun chooseInfluenceToSurrender(): Influence {
    return player.heldInfluences.random()
  }
}