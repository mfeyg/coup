package coup.game

import coup.game.ActionResponse.Allow
import coup.game.GameEvent.*
import coup.game.Permission.Companion.allow
import coup.server.newId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*

class Game(
  private val players: List<Player>,
  private val deck: Deck,
) {
  val id = newId

  constructor(board: Board) : this(board.players, board.deck)

  private val _events = MutableSharedFlow<GameEvent>(replay = UNLIMITED)
  val events get() = _events.asSharedFlow()

  private val _currentPlayer = MutableStateFlow(players.first())
  var currentPlayer
    get() = _currentPlayer.value
    private set(value) {
      _currentPlayer.value = value
    }
  val turns = _currentPlayer.asSharedFlow()

  private val activePlayers get() = players.filter { it.isActive }

  val scope = CoroutineScope(Dispatchers.Default)

  suspend fun start() {
    while (true) {
      if (activePlayers.size < 2) {
        emit(GameOver(activePlayers.first()))
        scope.cancel()
        break
      }
      takeTurn()
      nextPlayer()
    }
  }

  private val turnOrder = sequence { while (true) yieldAll(players) }

  private fun nextPlayer() {
    currentPlayer = turnOrder
      .dropWhile { it != currentPlayer }
      .drop(1)
      .dropWhile { !it.isActive }
      .first()
  }

  private suspend fun takeTurn(player: Player = currentPlayer) {
    emit(TurnStarted(player))
    val others = activePlayers - player
    val action = player.takeTurn(others)
    if (!action.incontestable) {
      emit(ActionAttempted(action))
    }
    when (val response = respond(others) { respondToAction(action) }) {
      Allow -> perform(action)
      is ActionResponse.Block -> {
        val (blocker, blockingInfluence) = response
        emit(BlockAttempted(action, blocker, blockingInfluence))
        val responseToBlock = respond(activePlayers - blocker) {
          respondToBlock(blocker, blockingInfluence)
        }
        if (responseToBlock is BlockResponse.Challenge) {
          val challenger = responseToBlock.challenger
          emit(BlockChallenged(action, blocker, blockingInfluence, challenger))
          val challengeResponse = blocker.respondToChallenge(blockingInfluence, challenger)
          emit(InfluenceRevealed(blocker, challengeResponse.influence))
          if (challengeResponse.influence == blockingInfluence) {
            emit(ActionBlocked(action, blocker, blockingInfluence))
            val lostInfluence = challenger.loseInfluence()
            emit(InfluenceSurrendered(challenger, lostInfluence))
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
          coroutineScope {
            launch { perform(action) }
            launch {
              val surrenderedInfluence = challenger.loseInfluence()
              emit(InfluenceSurrendered(challenger, surrenderedInfluence))
            }
          }
        }
      }
    }
  }

  private suspend fun emit(event: GameEvent) {
    _events.emit(event)
  }

  private suspend fun perform(action: Action) {
    emit(ActionPerformed(action))
    action.perform(deck)
  }

  private suspend inline fun <reified ResponseT : Permission> respond(
    players: Iterable<Player>,
    noinline respond: suspend Player.() -> ResponseT,
  ): ResponseT =
    players.map { player -> flow { emit(respond(player)) } }.merge().firstOrNull { !it.allowed } ?: allow()
}