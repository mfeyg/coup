package coup.game

import coup.game.Permission.Companion.allow
import kotlinx.coroutines.flow.*

class Player(val name: String, val playerNumber: Int, private val agent: Agent) {
  interface Agent {
    data class ActionChoice(val actionType: Action.Type, val validTargets: List<Player>? = null)

    suspend fun chooseAction(player: Player, choices: List<ActionChoice>): Action
    suspend fun respondToAction(player: Player, action: Action): ActionResponse
    suspend fun respondToBlock(player: Player, blocker: Player, influence: Influence): BlockResponse
    suspend fun respondToChallenge(player: Player, claim: Influence, challenger: Player): ChallengeResponse
    suspend fun surrenderInfluence(player: Player): Influence
    suspend fun exchange(player: Player, drawnInfluences: List<Influence>): List<Influence>
  }

  private data class State(val isk: Int, val heldInfluences: List<Influence>, val revealedInfluences: List<Influence>)

  private val state = MutableStateFlow(State(0, emptyList(), emptyList()))
  val updates = state.map { this }

  val isk get() = state.value.isk
  val heldInfluences get() = state.value.heldInfluences
  val revealedInfluences get() = state.value.revealedInfluences

  val isActive get() = heldInfluences.isNotEmpty()

  fun gainIsk(amount: Int) {
    state.update { it.copy(isk = it.isk + amount) }
  }

  fun loseIsk(amount: Int) {
    state.update { it.copy(isk = it.isk - amount) }
  }

  private fun draw(vararg influences: Influence) {
    state.update { it.copy(heldInfluences = it.heldInfluences + influences) }
  }

  suspend fun loseInfluence(): Influence? {
    if (heldInfluences.isEmpty()) return null
    val influence = agent.surrenderInfluence(this)
    return influence.also { loseInfluence(it) }
  }

  private fun loseInfluence(influence: Influence) {
    state.update {
      it.copy(
        revealedInfluences = it.revealedInfluences + influence,
        heldInfluences = it.heldInfluences - influence,
      )
    }
  }

  fun drawFrom(deck: Deck) = draw(deck.draw())

  suspend fun exchangeWith(deck: Deck) {
    val influences = listOf(deck.draw(), deck.draw())
    val toReturn = agent.exchange(this, influences)
    val heldInfluences = (heldInfluences + influences).toMutableList()
    toReturn.forEach {
      heldInfluences -= it
      deck.putBack(it)
    }
    deck.shuffle()
    state.update { it.copy(heldInfluences = heldInfluences) }
  }

  fun swapOut(influence: Influence, deck: Deck) {
    val heldInfluences = heldInfluences.toMutableList()
    heldInfluences -= influence
    deck.putBack(influence)
    deck.shuffle()
    heldInfluences += deck.draw()
    state.update { it.copy(heldInfluences = heldInfluences) }
  }

  suspend fun takeTurn(validTargets: List<Player>): Action {
    val availableActions =
      if (isk >= 10) listOf(Action.Type.Coup)
      else Action.Type.entries.filter { action -> action.cost <= isk }
    return agent.chooseAction(this, availableActions.map { type ->
      if (type.hasTarget) Agent.ActionChoice(type, validTargets)
      else Agent.ActionChoice(type)
    })
  }

  suspend fun respondToAction(action: Action) =
    if (action.incontestable) allow() else
      agent.respondToAction(this, action)

  suspend fun respondToBlock(blocker: Player, influence: Influence): BlockResponse =
    agent.respondToBlock(this, blocker, influence)

  suspend fun respondToChallenge(claim: Influence, challenger: Player): ChallengeResponse {
    val response = agent.respondToChallenge(this, claim, challenger)
    val influence = response.influence
    if (influence != claim) {
      loseInfluence(influence)
    }
    return response
  }

  override fun toString() = name
}