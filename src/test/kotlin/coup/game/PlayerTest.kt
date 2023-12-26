package coup.game

import coup.game.Player.Companion.reaction
import coup.game.actions.Action
import coup.game.rules.StandardRules
import io.mockk.coEvery
import io.mockk.coJustAwait
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerTest {
  private val prompts = mutableMapOf<Player, Agent>()
  private val rules = StandardRules()

  private fun mockPlayer(num: Int): Player {
    val agent = mockk<Agent>()
    val player = Player("Player $num", num, rules) { agent }
    prompts[player] = agent
    return player
  }

  private fun promptsFor(player: Player, block: Agent.() -> Unit) = with(prompts[player]!!, block)

  private val player1 = mockPlayer(1)
  private val player2 = mockPlayer(2)
  private val player3 = mockPlayer(3)

  @Test
  fun respondToAction_allow() = runBlocking {
    val players = listOf(player1, player2, player3)
    val action = Action.Assassinate(player1, player2, 7)
    promptsFor(player2) { coEvery { chooseReaction(action) } returns Reaction.Allow }
    promptsFor(player3) { coEvery { chooseReaction(action) } returns Reaction.Allow }
    assertEquals(players.reaction(action), Reaction.Allow)
  }

  @Test
  fun respondToAction_block() = runBlocking {
    val players = listOf(player1, player2, player3)
    val action = Action.Assassinate(player1, player2, 3)
    promptsFor(player2) {
      coEvery { chooseReaction(action) } returns Reaction.Block(player2, Influence.Contessa)
    }
    promptsFor(player3) { coJustAwait { chooseReaction(action) } }
    assertEquals(players.reaction(action), Reaction.Block(player2, Influence.Contessa))
  }
}