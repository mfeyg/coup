package coup.game

import coup.game.Influence.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import kotlin.test.*

class GameTest {

  val timeout = 1_000L

  val playerOne = MockPlayer("Player One", timeout)
  val playerTwo = MockPlayer("Player Two", timeout)
  val playerThree = MockPlayer("Player Three", timeout)

  var players: List<MockPlayer> = listOf(playerOne, playerTwo, playerThree)
  var deck = Deck.standard()

  private suspend fun withGame(game: Game = Game(players, deck), block: suspend (Game) -> Unit) =
    coroutineScope {
      val job = launch { game.start() }
      block.invoke(game)
      job.cancelAndJoin()
    }

  private suspend fun drawCards(players: List<Player> = this.players) {
    players.forEach { player -> repeat(2) { player.drawFrom(deck) } }
  }

  private suspend fun drawCards(vararg players: Player) = drawCards(players.asList())

  private suspend fun Game.awaitTurn(matcher: (String) -> Boolean) {
    try {
      withTimeout(timeout) {
        events.filterIsInstance<GameEvent.TurnStarted>()
          .first { matcher(it.player) }
      }
    } catch (e: TimeoutCancellationException) {
      fail { "Timed out waiting for next turn." }
    }
  }

  private suspend fun Game.awaitTurn(player: MockPlayer) = awaitTurn { it == player.name }

  suspend fun permit(actingPlayer: MockPlayer) =
    (players - actingPlayer).forEach { player -> player.allow() }

  @Test
  fun takeTurn_providesValidTargets() = runBlocking(Dispatchers.Default) {
    players = listOf(playerOne, playerTwo, playerThree)
    drawCards(players)
    withGame {
      playerOne.onTakeTurn { targets ->
        assertEquals(listOf(playerTwo, playerThree), targets)
        Action.Income(playerOne)
      }
    }
  }

  @Test
  fun playersWithNoCardsCannotBeTargeted() = runBlocking(Dispatchers.Default) {
    players = listOf(playerOne, playerTwo, playerThree)
    drawCards(playerOne, playerTwo)
    withGame {
      playerOne.onTakeTurn { targets ->
        assertEquals(listOf(playerTwo), targets)
        Action.Income(playerOne)
      }
    }
  }


  @Test
  fun permittedActionsArePerformed() = runBlocking(Dispatchers.Default) {
    drawCards()
    withGame { game ->
      playerThree.gainIsk(3)
      playerOne.stealFrom(playerThree)
      permit(playerOne)
      game.awaitTurn(playerTwo)

      assertEquals(2, playerOne.isk)
      assertEquals(1, playerThree.isk)
    }
  }

  @Test
  fun blockedActionsAreNotPerformed() = runBlocking(Dispatchers.Default) {
    drawCards()
    withGame {
      playerOne.foreignAid()
      playerThree.blockAs(Duke)
      permit(playerThree)

      assertEquals(0, playerOne.isk)
    }
  }

  @Test
  fun aBlockEndsTheTurn() = runBlocking(Dispatchers.Default) {
    drawCards()
    withGame { game ->
      playerOne.foreignAid()
      playerThree.blockAs(Duke)
      permit(playerThree)
      game.awaitTurn(playerTwo)
      playerTwo.income()
    }
  }

  @Test
  fun block_canBeSuccessfullyChallenged() = runBlocking(Dispatchers.Default) {
    drawCards(playerOne, playerThree)
    playerTwo.draw(Duke, Contessa)
    withGame { game ->
      playerOne.foreignAid()
      playerTwo.blockAs(Duke)
      playerOne.challenge()
      playerTwo.reveal(Contessa)
      game.awaitTurn(playerTwo)

      assertEquals(2, playerOne.isk)
    }
  }

  @Test
  fun block_canBeUnsuccessfullyChallenged() = runBlocking(Dispatchers.Default) {
    drawCards(playerOne, playerThree)
    playerTwo.draw(Duke, Contessa)
    withGame {
      playerOne.foreignAid()
      playerTwo.blockAs(Duke)
      playerOne.challenge()
      playerTwo.reveal(Duke)

      assertEquals(0, playerOne.isk)
    }
  }

  @Test
  fun block_onSuccessfulChallenge_blockerLosesInfluence() = runBlocking(Dispatchers.Default) {
    drawCards(playerOne, playerThree)
    playerTwo.draw(Duke, Contessa)
    withGame { game ->
      playerOne.foreignAid()
      playerTwo.blockAs(Duke)
      playerOne.challenge()
      playerTwo.reveal(Contessa)
      game.awaitTurn(playerTwo)

      assertEquals(1, playerTwo.numInfluences)
    }
  }

  @Test
  fun block_onUnsuccessfulChallenge_challengerLosesInfluence() = runBlocking(Dispatchers.Default) {
    drawCards(playerOne, playerThree)
    playerTwo.draw(Duke, Contessa)
    withGame { game ->
      playerOne.foreignAid()
      playerTwo.blockAs(Duke)
      playerOne.challenge()
      playerTwo.reveal(Duke)
      game.awaitTurn(playerTwo)

      assertEquals(1, playerOne.numInfluences)
    }
  }

  @Test
  fun actions_canBeSuccessfullyChallenged() = runBlocking(Dispatchers.Default) {
    drawCards(playerTwo, playerThree)
    playerOne.draw(Duke, Assassin)
    withGame { game ->
      playerOne.assassinate(playerTwo)
      playerThree.challenge()
      playerOne.reveal(Duke)
      game.awaitTurn(playerTwo)
      assertEquals(2, playerTwo.numInfluences)
    }
  }

  @Test
  fun actions_canBeUnsuccessfullyChallenged() = runBlocking(Dispatchers.Default) {
    drawCards(playerTwo, playerThree)
    playerOne.draw(Duke, Assassin)
    withGame { game ->
      playerOne.assassinate(playerTwo)
      playerThree.challenge()
      playerOne.reveal(Assassin)
      game.awaitTurn(playerTwo)
      assertEquals(1, playerTwo.numInfluences)
    }
  }

  @Test
  fun action_onSuccessfulChallenge_playerLosesInfluence() = runBlocking(Dispatchers.Default) {
    drawCards(playerTwo, playerThree)
    playerOne.draw(Duke, Assassin)
    withGame { game ->
      playerOne.tax()
      playerTwo.challenge()
      playerOne.reveal(Assassin)
      game.awaitTurn(playerTwo)
      assertEquals(1, playerOne.numInfluences)
    }
  }

  @Test
  fun action_onUnsuccessfulChallenge_challengerLosesInfluence() = runBlocking(Dispatchers.Default) {
    drawCards(playerTwo, playerThree)
    playerOne.draw(Duke, Assassin)
    withGame { game ->
      playerOne.tax()
      playerTwo.challenge()
      playerOne.reveal(Duke)
      game.awaitTurn(playerTwo)
      assertEquals(1, playerTwo.numInfluences)
    }
  }
}