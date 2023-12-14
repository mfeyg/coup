package coup.game

import coup.game.Player.Companion.challenger
import coup.game.Player.Companion.reaction
import coup.game.rules.Ruleset
import kotlinx.coroutines.flow.*

class Game(private val ruleset: Ruleset, players: List<Player>) {

  private val board = ruleset.setUpBoard(players)

  private val activePlayers get() = board.activePlayers
  private val deck get() = board.deck

  private val currentTurn = MutableStateFlow(Turn(players))

  private fun nextTurn() = with(currentTurn) { value = value.next() }

  val currentPlayer: Player get() = currentTurn.value.currentPlayer
  val winner: Player? get() = activePlayers.singleOrNull()

  val updates = currentTurn.map {}

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
          if (revealedInfluence == blockingInfluence) {
            blocker.swapOut(blockingInfluence, deck)
            challenger.loseInfluence()
          } else {
            action.perform()
          }
        }
      }

      is Reaction.Challenge -> {
        val (challenger) = response
        val requiredInfluence = ruleset.requiredInfluence(action)!!
        val revealedInfluence = player.respondToChallenge(requiredInfluence, challenger)
        if (revealedInfluence == requiredInfluence) {
          player.swapOut(revealedInfluence, deck)
          challenger.loseInfluence()
          action.perform()
        }
      }
    }

    nextTurn()
  }
}