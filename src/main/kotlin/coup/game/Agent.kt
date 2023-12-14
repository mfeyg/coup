package coup.game

import coup.game.Reaction.Block
import coup.game.actions.Action

interface Agent {

   suspend fun chooseAction(board: Board): Action

   suspend fun chooseCardsToReturn(drawnCards: List<Influence>): List<Influence>

   suspend fun chooseReaction(action: Action): Reaction

   suspend fun chooseInfluenceToReveal(claimedInfluence: Influence, challenger: Player): Influence

   suspend fun chooseWhetherToChallenge(block: Block): Boolean

   suspend fun chooseInfluenceToSurrender(): Influence
}