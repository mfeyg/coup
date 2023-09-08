package coup.game

import coup.game.MockPlayer.Request.*
import coup.game.ActionResponse.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout

class MockPlayer(override val name: String, private val timeout: Long) : Player() {
  private val request = MutableStateFlow<Request?>(null)
  private val responses = Channel<Any>()

  private suspend fun awaitRequest() = withTimeout(timeout) { request.filterNotNull().first() }

  private sealed interface Request {
    data class TakeTurn(val validTargets: List<Player>) : Request
    data class RespondToAction(val action: Action) : Request
    data class RespondToBlock(val blocker: Player, val influence: Influence) : Request
    data object RespondToChallenge : Request
    data object SurrenderInfluence : Request
    data class ExchangeWithDeck(val deck: Deck) : Request
  }

  private suspend fun makeRequest(request: Request): Any {
    try {
      this.request.compareAndSet(null, request) ||
          throw IllegalStateException("There is already a pending request")
      return responses.receive()
    } finally {
      this.request.value = null
    }
  }

  override suspend fun takeTurn(validTargets: List<Player>) =
    makeRequest(TakeTurn(validTargets)) as Action

  override suspend fun respondToAction(action: Action) =
    makeRequest(RespondToAction(action)) as ActionResponse

  override suspend fun respondToBlock(blocker: Player, influence: Influence) =
    makeRequest(RespondToBlock(blocker, influence)) as ActionResponse

  override suspend fun respondToChallenge() = makeRequest(RespondToChallenge) as Influence

  override suspend fun surrenderInfluence() = heldInfluences.random()

  override suspend fun exchange(deck: Deck) {
    makeRequest(ExchangeWithDeck(deck))
  }

  suspend fun onTakeTurn(response: (List<Player>) -> Action) {
    val request = awaitRequest() as TakeTurn
    this.responses.send(response(request.validTargets))
  }

  suspend fun income() = onTakeTurn { Action.Income(this) }
  suspend fun foreignAid() = onTakeTurn { Action.ForeignAid(this) }
  suspend fun tax() = onTakeTurn { Action.Tax(this) }
  suspend fun stealFrom(player: Player) = onTakeTurn { Action.Steal(this, player) }
  suspend fun exchange() = onTakeTurn { Action.Exchange(this) }
  suspend fun assassinate(player: Player) = onTakeTurn { Action.Assassinate(this, player) }
  suspend fun coup(player: Player) = onTakeTurn { Action.Coup(this, player) }

  suspend fun allow() {
    awaitRequest()
    responses.send(Allow)
  }

  suspend fun blockAs(influence: Influence) {
    awaitRequest()
    val response = Block(this, influence)
    responses.send(response)
  }

  suspend fun challenge() {
    awaitRequest()
    val response = Challenge(this)
    responses.send(response)
  }

  suspend fun reveal(influence: Influence) {
    awaitRequest()
    responses.send(influence)
  }
}