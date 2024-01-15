package coup.game.actions

import coup.game.Deck
import coup.game.Player
import coup.game.actions.Action.Type.Companion.type
import kotlin.math.min

sealed class Action(val player: Player, private val cost: Int = 0, private val effect: suspend () -> Unit) {


  open val target: Player? = null

  private var _onPerform: Action.() -> Unit = {}
  fun onPerform(block: Action.() -> Unit) {
    _onPerform = block
  }

  suspend fun perform() {
    player.pay(cost)
    effect.invoke()
    _onPerform()
  }

  override fun toString() = buildString {
    append("Action.$type(")
    append("player=$player")
    target?.let {
      append(", ")
      append("target=$target")
    }
    append(")")
  }

  class Income(player: Player) : Action(player, effect = {
    player.gainIsk(1)
  })

  class ForeignAid(player: Player) : Action(player, effect = {
    player.gainIsk(2)
  })

  class Tax(player: Player) : Action(player, effect = {
    player.gainIsk(3)
  })

  class Steal(player: Player, override val target: Player, stealAmount: Int = 2) : Action(player, effect = {
    val amountStolen = min(target.isk, stealAmount)
    target.loseIsk(amountStolen)
    player.gainIsk(amountStolen)
  })

  class Exchange(player: Player, deck: Deck) : Action(player, effect = {
    player.exchangeWith(deck)
  })

  class Assassinate(player: Player, override val target: Player, cost: Int) : Action(player, cost, effect = {
    target.loseInfluence()
  })

  class Coup(player: Player, override val target: Player, cost: Int) : Action(player, cost, effect = {
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
