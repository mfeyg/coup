package coup.game.actions

import coup.game.Deck
import coup.game.Player
import kotlin.math.min

sealed class Action(val player: Player, private val effect: suspend () -> Unit) {

  open val target: Player? = null

  suspend fun perform() = effect()

  class Income(player: Player) : Action(player, {
    player.gainIsk(1)
  })

  class ForeignAid(player: Player) : Action(player, {
    player.gainIsk(2)
  })

  class Tax(player: Player) : Action(player, {
    player.gainIsk(3)
  })

  class Steal(player: Player, override val target: Player, stealAmount: Int = 2) : Action(player, {
    val amountStolen = min(target.isk, stealAmount)
    target.loseIsk(amountStolen)
    player.gainIsk(amountStolen)
  })

  class Exchange(player: Player, deck: Deck) : Action(player, {
    player.exchangeWith(deck)
  })

  class Assassinate(player: Player, override val target: Player, cost: Int) : Action(player, {
    player.pay(cost)
    target.loseInfluence()
  })

  class Coup(player: Player, override val target: Player, cost: Int) : Action(player, {
    player.pay(cost)
    target.loseInfluence()
  })

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
