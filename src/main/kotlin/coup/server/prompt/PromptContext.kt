package coup.server.prompt

import coup.game.Player
import coup.game.rules.Ruleset
import coup.server.GameOptions

class PromptContext(val player: Player, val ruleset: Ruleset, val options: GameOptions)