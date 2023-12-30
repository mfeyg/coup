package coup.server.agent

import coup.game.*
import coup.game.actions.Action
import coup.server.agent.ChooseAction.chooseAction
import coup.server.agent.ExchangeWithDeck.returnCards
import coup.server.agent.RespondToAction.respondToAction
import coup.server.agent.RespondToBlock.challengeBlock
import coup.server.agent.RespondToChallenge.respondToChallenge
import coup.server.agent.SurrenderInfluence.surrenderInfluence

class PlayerAgent(private val context: PromptContext) : Agent {
  override suspend fun chooseAction(board: Board) =
    context.chooseAction(board)

  override suspend fun chooseCardsToReturn(drawnCards: List<Influence>) =
    context.returnCards(drawnCards)

  override suspend fun chooseReaction(action: Action) =
    context.respondToAction(action)

  override suspend fun chooseInfluenceToReveal(claimedInfluence: Influence, challenger: Player) =
    context.respondToChallenge(claimedInfluence, challenger)

  override suspend fun chooseWhetherToChallenge(block: Reaction.Block) =
    context.challengeBlock(block)

  override suspend fun chooseInfluenceToSurrender() =
    context.surrenderInfluence()

}