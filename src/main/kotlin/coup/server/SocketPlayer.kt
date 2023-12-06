package coup.server

import coup.game.*
import coup.game.Player
import coup.game.Player.Agent.*
import coup.server.prompt.*

class SocketPlayer(private val ruleset: Ruleset, private val session: Session<*>) : Player.Agent {

  override suspend fun chooseAction(options: List<Ruleset.ActionBuilder>, targets: List<Player>): Action =
    session.prompt(TakeTurn(options, targets))

  override suspend fun respondToAction(player: Player, action: Action): ActionResponse {
    return session.prompt(RespondToAction(ruleset, player, action))
  }

  override suspend fun respondToBlock(player: Player, blocker: Player, influence: Influence): BlockResponse {
    return session.prompt(RespondToBlock(player, blocker, influence))
  }

  override suspend fun respondToChallenge(player: Player, claim: Influence, challenger: Player): ChallengeResponse {
    return session.prompt(RespondToChallenge(claim, challenger, player.heldInfluences))
  }

  override suspend fun surrenderInfluence(player: Player): Influence {
    return session.prompt(SurrenderInfluence(player.heldInfluences))
  }

  override suspend fun exchange(player: Player, drawnInfluences: List<Influence>): List<Influence> {
    return session.prompt(Exchange(heldInfluences = player.heldInfluences, drawnInfluences = drawnInfluences))
  }
}