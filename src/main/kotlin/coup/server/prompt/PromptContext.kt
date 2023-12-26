package coup.server.prompt

import coup.game.Player
import coup.game.rules.Ruleset
import coup.server.GameOptions
import coup.server.Prompt
import coup.server.PromptBuilder

class PromptContext(
  val player: Player,
  val ruleset: Ruleset,
  val options: GameOptions,
  val perform: Perform,
) {
  interface Perform {
    suspend operator fun <T> invoke(prompt: Prompt<T>): T
  }

  suspend fun <T> prompt(build: PromptBuilder<T>.() -> Unit) = perform(PromptBuilder.prompt(build))
}