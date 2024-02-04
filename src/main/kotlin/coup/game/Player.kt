package coup.game

import coup.game.Reaction.Block
import coup.game.actions.Action
import coup.game.rules.Ruleset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Player(
  val number: Int,
  val name: String,
  private val ruleset: Ruleset,
  agent: (Player) -> Agent,
) {
  private val agent = agent(this)

  private data class State(
    val isk: Int = 0,
    val heldInfluences: List<Influence> = emptyList(),
    val revealedInfluences: List<Influence> = emptyList(),
  )

  private val state = MutableStateFlow(State())
  val updates = state.map {}

  val isk get() = state.value.isk
  val heldInfluences get() = state.value.heldInfluences
  val revealedInfluences get() = state.value.revealedInfluences

  val isActive get() = heldInfluences.isNotEmpty()

  fun gainIsk(amount: Int) {
    state.update { it.copy(isk = it.isk + amount) }
  }

  fun pay(cost: Int) = loseIsk(cost)

  fun loseIsk(amount: Int) {
    state.update {
      require(amount <= it.isk)
      it.copy(isk = it.isk - amount)
    }
  }

  private fun draw(vararg influences: Influence) {
    state.update { it.copy(heldInfluences = it.heldInfluences + influences) }
  }

  suspend fun loseInfluence(): Influence? {
    if (heldInfluences.isEmpty()) return null
    val influence = if (heldInfluences.size == 1) heldInfluences[0] else agent.chooseInfluenceToSurrender()
    return influence.also { loseInfluence(it) }
  }

  private fun loseInfluence(influence: Influence) {
    state.update {
      require(influence in it.heldInfluences) { "$this doesn't have a $influence" }
      it.copy(
        revealedInfluences = it.revealedInfluences + influence,
        heldInfluences = it.heldInfluences - influence,
      )
    }
  }

  fun drawFrom(deck: Deck) = draw(deck.draw())

  suspend fun exchangeWith(deck: Deck) {
    val influences = listOf(deck.draw(), deck.draw())
    val toReturn = agent.chooseCardsToReturn(influences)
    val heldInfluences = (heldInfluences + influences).toMutableList()
    toReturn.forEach {
      check(it in heldInfluences)
      heldInfluences -= it
      deck.putBack(it)
    }
    deck.shuffle()
    state.update { it.copy(heldInfluences = heldInfluences) }
  }

  fun swapOut(influence: Influence, deck: Deck) {
    val heldInfluences = heldInfluences.toMutableList()
    require(influence in heldInfluences)
    heldInfluences -= influence
    deck.putBack(influence)
    deck.shuffle()
    heldInfluences += deck.draw()
    state.update { it.copy(heldInfluences = heldInfluences) }
  }

  suspend fun chooseAction(board: Board) = agent.chooseAction(board)

  private suspend fun respondToAction(action: Action) =
    if (!(ruleset.canChallenge(this, action) || ruleset.canAttemptBlock(this, action)))
      Reaction.Allow
    else
      agent.chooseReaction(action)

  private suspend fun challengesBlock(block: Block) =
    block.blocker != this && agent.chooseWhetherToChallenge(block)

  suspend fun respondToChallenge(claim: Influence, challenger: Player): Influence {
    val influence = agent.chooseInfluenceToReveal(claim, challenger)
    if (influence != claim) {
      loseInfluence(influence)
    }
    return influence
  }

  override fun toString() = "Player ${number + 1} <<$name>>"

  companion object {

    private suspend fun <T : Any> Collection<Player>.prompt(respond: suspend Player.() -> T?): T? {
      val response = CompletableDeferred<T?>()
      coroutineScope {
        launch {
          val outerScope = this
          forEach { responder ->
            launch {
              responder.respond()?.let {
                response.complete(it)
                outerScope.cancel()
              }
            }
          }
        }
      }
      response.complete(null)
      return response.await()
    }

    suspend fun Collection<Player>.reaction(action: Action) =
      prompt { respondToAction(action).takeIf { it != Reaction.Allow } } ?: Reaction.Allow

    suspend fun Collection<Player>.challenger(block: Block) = prompt { takeIf { challengesBlock(block) } }
  }
}