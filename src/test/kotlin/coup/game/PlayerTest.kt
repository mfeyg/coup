package coup.game

import coup.game.Influence.*
import coup.game.Player.Companion.reaction
import coup.game.actions.Action
import coup.game.rules.StandardRules
import io.mockk.coEvery
import io.mockk.coJustAwait
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerTest {
  private val agentForPlayer = mutableMapOf<Player, Agent>()
  private val rules = StandardRules()

  private fun mockPlayer(num: Int): Player {
    val agent = mockk<Agent>()
    val player = Player(num, "test player $num", rules) { agent }
    agentForPlayer[player] = agent
    return player
  }

  private fun withAgent(player: Player, block: Agent.() -> Unit) = with(agentForPlayer[player]!!, block)

  private val player1 = mockPlayer(1)
  private val player2 = mockPlayer(2)
  private val player3 = mockPlayer(3)

  @Test
  fun respondToAction_allow() = runBlocking {
    val players = listOf(player1, player2, player3)
    val action = Action.Assassinate(player1, player2, 7)
    withAgent(player2) { coEvery { chooseReaction(action) } returns Reaction.Allow }
    withAgent(player3) { coEvery { chooseReaction(action) } returns Reaction.Allow }
    assertEquals(players.reaction(action), Reaction.Allow)
  }

  @Test
  fun respondToAction_block() = runBlocking {
    val players = listOf(player1, player2, player3)
    val action = Action.Assassinate(player1, player2, 3)
    withAgent(player2) {
      coEvery { chooseReaction(action) } returns Reaction.Block(player2, Contessa)
    }
    withAgent(player3) { coJustAwait { chooseReaction(action) } }
    assertEquals(players.reaction(action), Reaction.Block(player2, Contessa))
  }

  @Test
  fun loseTwoInfluences() = runBlocking {
    val player = player1
    val deck = Deck(listOf(Assassin, Contessa))
    repeat(2) { player.drawFrom(deck) }
    withAgent(player) {
      coEvery { chooseInfluenceToSurrender() } coAnswers {
        delay(1)
        player.heldInfluences.first()
      }
    }
    coroutineScope {
      launch { player.loseInfluence() }
      launch { player.loseInfluence() }
    }
    assertEquals(emptyList(), player.heldInfluences)
  }
}