package coup.server

import coup.game.*
import coup.game.Player
import coup.game.Player.Agent.*
import coup.game.action.Action
import coup.server.prompt.Exchange.exchange
import coup.server.prompt.RespondToAction.respondToAction
import coup.server.prompt.RespondToBlock.respondToBlock
import coup.server.prompt.RespondToChallenge.respondToChallenge
import coup.server.prompt.SurrenderInfluence.surrenderInfluence
import coup.server.prompt.TakeTurn.takeTurn

class SocketPlayer(private val ruleset: Ruleset, private val session: Session<*>) : Player.Agent {

  override suspend fun chooseAction(player: Player, targets: List<Player>, ruleset: Ruleset) =
    session.takeTurn(player, targets, ruleset)

  override suspend fun respondToAction(player: Player, action: Action): ActionResponse =
    session.respondToAction(player, action, ruleset)

  override suspend fun respondToBlock(player: Player, blocker: Player, influence: Influence): BlockResponse =
    session.respondToBlock(blocker, influence)

  override suspend fun respondToChallenge(player: Player, claim: Influence, challenger: Player) =
    session.respondToChallenge(player, challenger, claim)

  override suspend fun surrenderInfluence(player: Player) = session.surrenderInfluence(player)

  override suspend fun exchange(player: Player, drawnInfluences: List<Influence>) =
    session.exchange(player, drawnInfluences)
}