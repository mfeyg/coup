package coup.server

import coup.game.*
import coup.game.Player
import coup.server.prompt.*

abstract class SocketPlayer : Player.Agent {
  abstract suspend fun <T> prompt(prompt: Prompt<T>): T
  override suspend fun takeTurn(player: Player, validTargets: List<Player>): Action {
    return prompt(TakeTurn(player, validTargets))
  }

  override suspend fun respondToAction(player: Player, action: Action): ActionResponse {
    return prompt(RespondToAction(player, action))
  }

  override suspend fun respondToBlock(player: Player, blocker: Player, influence: Influence): BlockResponse {
    return prompt(RespondToBlock(player, blocker, influence))
  }

  override suspend fun respondToChallenge(player: Player, claim: Influence, challenger: Player): ChallengeResponse {
    return prompt(RespondToChallenge(claim, challenger, player.heldInfluences))
  }

  override suspend fun surrenderInfluence(player: Player): Influence {
    return prompt(SurrenderInfluence(player.heldInfluences))
  }

  override suspend fun exchange(player: Player, influences: List<Influence>): List<Influence> {
    return prompt(Exchange(influences, player.heldInfluences))
  }
}