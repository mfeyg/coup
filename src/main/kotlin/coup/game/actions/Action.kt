package coup.game.actions

import coup.game.Deck
import coup.game.Player
import kotlin.math.min

sealed class Action(val player: Player, val cost: Int = 0) {

  abstract suspend fun perform()

  class Income(player: Player) : Action(player) {
    override suspend fun perform() = player.gainIsk(1)
  }

  class ForeignAid(player: Player) : Action(player) {
    override suspend fun perform() = player.gainIsk(2)
  }

  class Tax(player: Player) : Action(player) {
    override suspend fun perform() = player.gainIsk(3)
  }

  class Steal(player: Player, override val target: Player) : Action(player) {
    override suspend fun perform() {
      val amount = min(target.isk, 2)
      target.loseIsk(amount)
      player.gainIsk(amount)
    }
  }

  class Exchange(player: Player, private val deck: Deck) : Action(player) {
    override suspend fun perform() = player.exchangeWith(deck)
  }

  class Assassinate(player: Player, override val target: Player, cost: Int) : Action(player, cost) {
    override suspend fun perform() {
      player.pay(cost)
      target.loseInfluence()
    }
  }

  class Coup(player: Player, override val target: Player, cost: Int) : Action(player, cost) {
    override suspend fun perform() {
      player.pay(cost)
      target.loseInfluence()
    }
  }

  open val target: Player? get() = when (this) {
    is Assassinate -> target
    is Coup -> target
    is Steal -> target
    else -> null
  }
}