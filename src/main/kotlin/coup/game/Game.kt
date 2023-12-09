package coup.game

import coup.game.Player.Agent.ActionResponse
import coup.game.Player.Agent.BlockResponse
import coup.game.actions.Action
import coup.game.rules.Ruleset
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Game(private val ruleset: Ruleset, players: List<Player>) {

  private val board = ruleset.setUpBoard(players)

  private val players get() = board.players
  private val deck get() = board.deck

  private suspend fun Action.perform() = perform(board)

  private val _currentPlayer = MutableStateFlow(players.first())

  val currentPlayer = _currentPlayer.asStateFlow()
  private val _winner = MutableStateFlow<Player?>(null)

  val winner = _winner.asStateFlow()

  private val activePlayers get() = players.filter { it.isActive }

  suspend fun start() {
    while (true) {
      if (activePlayers.size < 2) {
        _winner.value = activePlayers.first()
        break
      }
      takeTurn()
      nextPlayer()
    }
  }

  private val turnOrder = sequence { while (true) yieldAll(players) }

  private fun nextPlayer() {
    _currentPlayer.value = turnOrder
      .dropWhile { it != currentPlayer.value }
      .drop(1)
      .dropWhile { !it.isActive }
      .first()
  }

  private suspend fun takeTurn(player: Player = currentPlayer.value) {
    val others = activePlayers - player
    val action = player.takeTurn(others)
    when (val response = attemptAction(action, others)) {
      is ActionResponse.Allow -> action.perform()

      is ActionResponse.Block -> {
        val block = response.block
        val (blocker, blockingInfluence) = block
        val challenger = attemptBlock(block, activePlayers - blocker)
        if (challenger != null) {
          val challengeResponse = blocker.respondToChallenge(blockingInfluence, challenger)
          if (challengeResponse.influence == blockingInfluence) {
            blocker.swapOut(blockingInfluence, deck)
            challenger.loseInfluence()
          } else {
            action.perform()
          }
        }
      }

      is ActionResponse.Challenge -> {
        val (challenger) = response
        val (influence) = player.respondToChallenge(ruleset.requiredInfluence(action)!!, challenger)
        if (influence == ruleset.requiredInfluence(action)) {
          player.swapOut(influence, deck)
          challenger.loseInfluence()
          action.perform()
        }
      }
    }
  }

  private suspend fun attemptAction(
    action: Action,
    responders: Iterable<Player>
  ) = channelFlow {
    for (responder in responders) launch {
      send(responder.respondToAction(action))
    }
  }.firstOrNull { it != ActionResponse.Allow } ?: ActionResponse.Allow

  private suspend fun attemptBlock(
    block: Block,
    responders: Iterable<Player>,
  ) = channelFlow {
    for (responder in responders) launch {
      if (responder.respondToBlock(block) == BlockResponse.Challenge) {
        send(responder)
      }
    }
  }.firstOrNull()
}