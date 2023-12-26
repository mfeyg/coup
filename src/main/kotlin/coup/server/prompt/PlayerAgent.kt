package coup.server.prompt

import coup.game.*
import coup.game.actions.Action
import coup.server.prompt.ChooseAction.chooseAction
import coup.server.prompt.ExchangeWithDeck.returnCards
import coup.server.prompt.RespondToAction.respondToAction
import coup.server.prompt.RespondToBlock.challengeBlock
import coup.server.prompt.RespondToChallenge.respondToChallenge
import coup.server.prompt.SurrenderInfluence.surrenderInfluence

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