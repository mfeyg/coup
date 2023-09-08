package coup.game

import coup.game.Permission.Companion.allow
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Player(val name: String, val playerNumber: Int, private val agent: Agent) {
  interface Agent {
    suspend fun takeTurn(validTargets: List<Player>): Action
    suspend fun respondToAction(action: Action): ActionResponse
    suspend fun respondToBlock(blocker: Player, influence: Influence): BlockResponse
    suspend fun respondToChallenge(claim: Influence, challenger: Player): ChallengeResponse
    suspend fun surrenderInfluence(): Influence
    suspend fun exchange(influences: List<Influence>): List<Influence>
  }

  private val _updates =
    MutableSharedFlow<Player>(replay = 1, onBufferOverflow = DROP_OLDEST)
  val updates = _updates.asSharedFlow()

  private fun update() = _updates.tryEmit(this)

  var isk: Int = 0
    private set
  var heldInfluences: List<Influence> = emptyList()
    private set
  var revealedInfluences: List<Influence> = emptyList()
    private set

  val isActive get() = heldInfluences.isNotEmpty()

  fun gainIsk(amount: Int) {
    isk += amount
    update()
  }

  fun loseIsk(amount: Int) {
    isk -= amount
    update()
  }

  fun draw(vararg influences: Influence) {
    heldInfluences += influences
    update()
  }

  suspend fun loseInfluence(): Influence = agent.surrenderInfluence()
    .also { loseInfluence(it) }

  private fun loseInfluence(influence: Influence) {
    heldInfluences -= influence
    revealedInfluences += influence
    update()
  }

  fun drawFrom(deck: Deck) = draw(deck.draw())

  suspend fun exchangeWith(deck: Deck) {
    val influences = listOf(deck.draw(), deck.draw())
    val toReturn = agent.exchange(influences)
    heldInfluences += influences
    toReturn.forEach {
      heldInfluences -= it
      deck.putBack(it)
    }
    update()
  }

  suspend fun takeTurn(validTargets: List<Player>): Action = agent.takeTurn(validTargets)
  suspend fun respondToAction(action: Action) =
    if (action.incontestable) allow() else
      agent.respondToAction(action)

  suspend fun respondToBlock(blocker: Player, influence: Influence): BlockResponse =
    agent.respondToBlock(blocker, influence)

  suspend fun respondToChallenge(claim: Influence, challenger: Player): ChallengeResponse {
    val response = agent.respondToChallenge(claim, challenger)
    val influence = response.influence
    if (influence != claim) {
      loseInfluence(influence)
    }
    return response
  }

  override fun toString() = name
}