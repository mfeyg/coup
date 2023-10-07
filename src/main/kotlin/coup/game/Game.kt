package coup.game

import coup.game.GameEvent.*
import coup.game.Player.Agent.ActionResponse
import coup.game.Player.Agent.BlockResponse
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Game(
  private val players: List<Player>,
  private val deck: Deck,
) {

  constructor(board: Board) : this(board.players, board.deck)

  private val _events = MutableSharedFlow<GameEvent>(replay = UNLIMITED)
  val events get() = _events.asSharedFlow()

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
        emit(GameOver(winner))
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
    emit(TurnStarted(player))
    val others = activePlayers - player
    val action = player.takeTurn(others)
    if (!action.incontestable) {
      emit(ActionAttempted(action))
    }
    when (val response = respond(ActionResponse.Allow, others) { respondToAction(action) }) {
      ActionResponse.Allow -> perform(action)
      is ActionResponse.Block -> {
        val (blocker, blockingInfluence) = response
        emit(BlockAttempted(action, blocker, blockingInfluence))
        val responseToBlock = respond(BlockResponse.Allow, activePlayers - blocker) {
          respondToBlock(blocker, blockingInfluence)
        }
        if (responseToBlock is BlockResponse.Challenge) {
          val challenger = responseToBlock.challenger
          emit(BlockChallenged(action, blocker, blockingInfluence, challenger))
          val challengeResponse = blocker.respondToChallenge(blockingInfluence, challenger)
          emit(InfluenceRevealed(blocker, challengeResponse.influence))
          if (challengeResponse.influence == blockingInfluence) {
            blocker.swapOut(blockingInfluence, deck)
            emit(ActionBlocked(action, blocker, blockingInfluence))
            val lostInfluence = challenger.loseInfluence()
            lostInfluence?.let { emit(InfluenceSurrendered(challenger, lostInfluence)) }
          } else {
            perform(action)
          }
        } else {
          emit(ActionBlocked(action, blocker, blockingInfluence))
        }
      }

      is ActionResponse.Challenge -> {
        val (challenger) = response
        emit(ActionChallenged(action, challenger))
        val (influence) = player.respondToChallenge(action.type.neededInfluence!!, challenger)
        if (influence == action.type.neededInfluence) {
          player.swapOut(influence, deck)
          val surrenderedInfluence = challenger.loseInfluence()
          surrenderedInfluence?.let { emit(InfluenceSurrendered(challenger, surrenderedInfluence)) }
          perform(action)
        }
      }
    }
  }

  private suspend fun emit(event: GameEvent) = _events.emit(event)

  private suspend fun perform(action: Action) {
    emit(ActionPerformed(action))
    action.perform(deck)
  }

  private suspend fun <ResponseT> respond(
    allow: ResponseT,
    players: Iterable<Player>,
    respond: suspend Player.() -> ResponseT
  ): ResponseT {
    return channelFlow {
      for (player in players) launch {
        send(player.respond())
      }
    }.firstOrNull { it != allow } ?: allow
  }
}