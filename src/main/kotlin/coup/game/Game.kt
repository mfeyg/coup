package coup.game

import coup.game.Player.Agent.ActionResponse
import coup.game.Player.Agent.BlockResponse
import coup.game.actions.Action
import coup.game.rules.Ruleset
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Game(private val ruleset: Ruleset, players: List<Player>) {

  private val board = ruleset.setUpBoard(players)

  private val players get() = board.activePlayers
  private val deck get() = board.deck

  private suspend fun Action.perform() = perform(board)

  private val playerOrder = sequence { while (true) yieldAll(players) }

  private fun nextPlayer(afterPlayer: Player? = null): Player {
    var mustHaveSeen = afterPlayer
    return playerOrder.first { player ->
      if (player == mustHaveSeen) {
        mustHaveSeen = null
        return@first false
      }
      mustHaveSeen == null && player.isActive
    }
  }

  private val _currentPlayer = MutableStateFlow(nextPlayer())
  val currentPlayer = _currentPlayer.asStateFlow()

  val winner: Player? get() = players.singleOrNull()

  suspend fun start() {
    while (winner == null) takeTurn()
  }

  private suspend fun takeTurn() {
    val player = currentPlayer.value
    val action = player.chooseAction(validTargets = players - player)

    when (val response = response(action)) {
      is ActionResponse.Allow -> action.perform()

      is ActionResponse.Block -> {
        val (block) = response
        val (blocker, blockingInfluence) = block
        val challenger = challenger(block)
        if (challenger != null) {
          val (revealedInfluence) = blocker.respondToChallenge(blockingInfluence, challenger)
          if (revealedInfluence == blockingInfluence) {
            blocker.swapOut(blockingInfluence, deck)
            challenger.loseInfluence()
          } else {
            action.perform()
          }
        }
      }

      is ActionResponse.Challenge -> {
        val (challenger) = response
        val requiredInfluence = ruleset.requiredInfluence(action)!!
        val (revealedInfluence) = player.respondToChallenge(requiredInfluence, challenger)
        if (revealedInfluence == requiredInfluence) {
          player.swapOut(revealedInfluence, deck)
          challenger.loseInfluence()
          action.perform()
        }
      }
    }

    _currentPlayer.value = nextPlayer(player)
  }

  private suspend fun response(action: Action): ActionResponse {
    val responders = players - action.player
    return channelFlow {
      for (responder in responders) launch {
        send(responder.respondToAction(action))
      }
    }.firstOrNull { it != ActionResponse.Allow } ?: ActionResponse.Allow
  }

  private suspend fun challenger(block: Block): Player? {
    val responders = players - block.blocker
    return channelFlow {
      for (responder in responders) launch {
        if (responder.respondToBlock(block) == BlockResponse.Challenge) {
          send(responder)
        }
      }
    }.firstOrNull()
  }
}