package coup.game

import coup.game.Player.Agent.ActionResponse
import coup.game.Player.Agent.BlockResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Game(
  private val players: List<Player>,
  private val deck: Deck,
  private val ruleset: Ruleset,
) {

  constructor(ruleset: Ruleset, board: Board) : this(board.players, board.deck, ruleset)

  private val board = Board(players, deck)

  private val _currentPlayer = MutableStateFlow(players.first())
  val currentPlayer = _currentPlayer.asStateFlow()

  private val _winner = MutableStateFlow<Player?>(null)
  val winner = _winner.asStateFlow()

  private val activePlayers get() = players.filter { it.isActive }

  suspend fun start() {
    while (true) {
      if (activePlayers.size < 2) {
        val winner = activePlayers.first()
        _winner.value = winner
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
    when (val response = respondToAction(others, action)) {
      ActionResponse.Allow -> perform(action)
      is ActionResponse.Block -> {
        val (blocker, blockingInfluence) = response
        val challenger = respondToBlock( activePlayers - blocker, blocker, blockingInfluence)
        if (challenger != null) {
          val challengeResponse = blocker.respondToChallenge(blockingInfluence, challenger)
          if (challengeResponse.influence == blockingInfluence) {
            blocker.swapOut(blockingInfluence, deck)
            challenger.loseInfluence()
          } else {
            perform(action)
          }
        }
      }

      is ActionResponse.Challenge -> {
        val (challenger) = response
        val (influence) = player.respondToChallenge(ruleset.requiredInfluence(action)!!, challenger)
        if (influence == ruleset.requiredInfluence(action)) {
          player.swapOut(influence, deck)
          challenger.loseInfluence()
          perform(action)
        }
      }
    }
  }

  private suspend fun perform(action: Action) {
    action.perform(board)
  }

  private suspend fun respondToAction(
    players: Iterable<Player>,
    action: Action
  ) = channelFlow {
    for (player in players) launch {
      send(player.respondToAction(action))
    }
  }.firstOrNull { it != ActionResponse.Allow } ?: ActionResponse.Allow

  private suspend fun respondToBlock(
    players: Iterable<Player>,
    blocker: Player,
    blockingInfluence: Influence,
  ) = channelFlow {
    for (player in players) launch {
      if (player.respondToBlock(blocker, blockingInfluence) == BlockResponse.Challenge) {
        send(player)
      }
    }
  }.firstOrNull()
}