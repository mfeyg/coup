package coup.server

import coup.game.*
import coup.game.actions.Action
import io.ktor.util.logging.*
import org.slf4j.LoggerFactory

class LoggingAgent(private val agent: Agent) : Agent {
  private val logger: Logger = LoggerFactory.getLogger(LoggingAgent::class.java)

  override suspend fun chooseAction(board: Board): Action {
    return agent.chooseAction(board).also { it.onPerform { logger.info("Performing $this") } }
  }

  override suspend fun chooseCardsToReturn(drawnCards: List<Influence>): List<Influence> {
    return agent.chooseCardsToReturn(drawnCards)
  }

  override suspend fun chooseReaction(action: Action): Reaction {
    return agent.chooseReaction(action)
  }

  override suspend fun chooseInfluenceToReveal(claimedInfluence: Influence, challenger: Player): Influence {
    return agent.chooseInfluenceToReveal(claimedInfluence, challenger)
  }

  override suspend fun chooseWhetherToChallenge(block: Reaction.Block): Boolean {
    return agent.chooseWhetherToChallenge(block)
  }

  override suspend fun chooseInfluenceToSurrender(): Influence {
    return agent.chooseInfluenceToSurrender()
  }
}