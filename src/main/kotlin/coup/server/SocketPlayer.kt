package coup.server

import coup.game.*
import coup.game.Player
import coup.server.message.Message
import coup.server.prompt.*

class SocketPlayer(
  val socket: Socket,
  player: () -> Player,
) : Player.Agent {
  val player by lazy(player)

  suspend fun send(message: Message) = socket.send(message)

  override suspend fun takeTurn(validTargets: List<Player>): Action {
    return socket.prompt(TakeTurn(player, validTargets))
  }

  override suspend fun respondToAction(action: Action): ActionResponse {
    return socket.prompt(RespondToAction(player, action))
  }

  override suspend fun respondToBlock(blocker: Player, influence: Influence): BlockResponse {
    return socket.prompt(RespondToBlock(player, blocker, influence))
  }

  override suspend fun respondToChallenge(claim: Influence, challenger: Player): ChallengeResponse {
    return socket.prompt(RespondToChallenge(claim, challenger))
  }

  override suspend fun surrenderInfluence(): Influence {
    return socket.prompt(SurrenderInfluence())
  }

  override suspend fun exchange(influences: List<Influence>): List<Influence> {
    return socket.prompt(Exchange(influences))
  }
}