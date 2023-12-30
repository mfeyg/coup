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

  private val currentTurn = MutableStateFlow(Turn(activePlayers))

  private fun nextTurn() = with(currentTurn) { value = value.next() }

  val currentPlayer: Player get() = currentTurn.value.currentPlayer
  val winner: Player? get() = activePlayers.singleOrNull()

  val players by board::players
  val updates = combine(board.players.map { it.updates } + currentTurn) {}

  suspend fun play() {
    while (winner == null) takeTurn()
  }

  private suspend fun takeTurn() {
    val player = currentPlayer
    val action = player.chooseAction(board)

    when (val response = activePlayers.reaction(action)) {
      is Reaction.Allow -> action.perform()

      is Reaction.Block -> {
        val (blocker, blockingInfluence) = response
        val challenger = activePlayers.challenger(response)
        if (challenger != null) {
          val revealedInfluence = blocker.respondToChallenge(blockingInfluence, challenger)
          if (revealedInfluence == blockingInfluence) coroutineScope {
            launch { blocker.swapOut(blockingInfluence, deck) }
            launch { challenger.loseInfluence() }
          } else {
            action.perform()
          }
        }
      }

      is Reaction.Challenge -> {
        val (challenger) = response
        val requiredInfluence = ruleset.requiredInfluence(action)!!
        val revealedInfluence = player.respondToChallenge(requiredInfluence, challenger)
        if (revealedInfluence == requiredInfluence) coroutineScope {
          launch { player.swapOut(revealedInfluence, deck) }
          launch { challenger.loseInfluence() }
          launch { action.perform() }
        }
      }
    }

    nextTurn()
  }
}