package coup.game.actions

import coup.game.Deck
import coup.game.Player
import kotlin.math.min

sealed class Action(val player: Player) {

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
    private val stealAmount = 2

    override suspend fun perform() {
      val amount = min(target.isk, stealAmount)
      target.loseIsk(amount)
      player.gainIsk(amount)
    }
  }

  class Exchange(player: Player, private val deck: Deck) : Action(player) {
    override suspend fun perform() = player.exchangeWith(deck)
  }

  class Assassinate(player: Player, override val target: Player, private val cost: Int) : Action(player) {
    override suspend fun perform() {
      player.pay(cost)
      target.loseInfluence()
    }
  }

  class Coup(player: Player, override val target: Player, private val cost: Int) : Action(player) {
    override suspend fun perform() {
      player.pay(cost)
      target.loseInfluence()
    }
  }

  open val target: Player? = null

  enum class Type {
    Income, ForeignAid, Tax, Steal, Exchange, Assassinate, Coup;

    companion object {
      val Action.type: Type
        get() = when (this) {
          is Action.Assassinate -> Assassinate
          is Action.Coup -> Coup
          is Action.Exchange -> Exchange
          is Action.ForeignAid -> ForeignAid
          is Action.Income -> Income
          is Action.Steal -> Steal
          is Action.Tax -> Tax
        }
    }
  }
}
