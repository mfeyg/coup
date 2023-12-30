package coup.server.agent

import coup.game.Player
import coup.game.rules.Ruleset
import coup.server.*

class PromptContext(
  val player: Player,
  val ruleset: Ruleset,
  val players: List<Person>,
  val options: GameOptions,
  private val session: Session<*, *>,
) {
  suspend fun <T> prompt(build: PromptBuilder<T>.() -> Unit) = session.prompt(PromptBuilder.prompt(build))
}