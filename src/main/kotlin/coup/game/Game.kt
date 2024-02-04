package coup.game

import coup.game.Player.Companion.challenger
import coup.game.Player.Companion.reaction
import coup.game.rules.Ruleset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Game(private val ruleset: Ruleset, private val board: Board) {

  private val activePlayers by board::activePlayers
  private val deck by board::deck
  private val gameLog = GameLog()

  private val currentTurn = MutableStateFlow(TurnProgression(activePlayers))

  private fun endTurn() = with(currentTurn) { value = value.next() }

  val activePlayer: Player get() = currentTurn.value.player
  val winner: Player? get() = activePlayers.singleOrNull()

  val players by board::players

  val updates = combine<Unit, Unit>(board.updates, currentTurn.map {}) {}

  suspend fun play() {
    while (winner == null) takeTurn()
    gameLog.logEvent("Game over") { "winner" `is` winner!! }
  }

  private suspend fun takeTurn() = gameLog.logScope {
    val player = activePlayer
    logContext { "player" `is` player }
    logEvent("New turn")

    val action = logContext("action") { player.chooseAction(board) }
    logEvent("Action selected")

    when (val reaction = activePlayers.reaction(action)) {
      is Reaction.Allow -> {
        logEvent("Action allowed")
        logEvent("Action performed")
        action.perform()
      }

      is Reaction.Block -> {
        val block = logContext("block") { reaction }
        val (blocker, blockingInfluence) = block
        logEvent("Block attempted")
        val challenger = logContext("challenger") { activePlayers.challenger(block) }
        if (challenger != null) {
          logEvent("Block challenged")
          val revealedInfluence = blocker.respondToChallenge(blockingInfluence, challenger)
          logEvent("Blocker revealed influence") { "influence" `is` revealedInfluence }
          if (revealedInfluence == blockingInfluence) coroutineScope {
            logEvent("Block validated")
            launch {
              logEvent("Blocker swaps out influence") { "influence" `is` revealedInfluence }
              blocker.swapOut(blockingInfluence, deck)
            }
            launch {
              challenger.loseInfluence()?.let { influence ->
                logEvent("Challenger surrenders influence") { "influence" `is` influence }
              }
            }
          } else {
            logEvent("Blocker lost influence") { "influence" `is` revealedInfluence }
            logEvent("Action performed")
            action.perform()
          }
        }
      }

      is Reaction.Challenge -> {
        val challenger = logContext("challenger") { reaction.challenger }
        logEvent("Action challenged")
        val requiredInfluence = ruleset.requiredInfluence(action)!!
        val revealedInfluence = player.respondToChallenge(requiredInfluence, challenger)
        logEvent("Player revealed influence") { "influence" `is` revealedInfluence }
        if (revealedInfluence == requiredInfluence) coroutineScope {
          logEvent("Action validated")
          launch {
            logEvent("Player swaps out influence") { "influence" `is` revealedInfluence }
            player.swapOut(revealedInfluence, deck)
          }
          launch {
            challenger.loseInfluence()?.let { influence ->
              logEvent("Challenger surrenders influence") { "influence" `is` influence }
            }
          }
          launch {
            logEvent("Action performed")
            action.perform()
          }
        } else {
          logEvent("Player lost influence") { "influence" `is` revealedInfluence }
        }
      }
    }

    endTurn()
  }
}