package coup.game

import coup.game.Action.Type.BlockableBy.Anyone
import coup.game.Action.Type.BlockableBy.NoOne
import coup.game.Action.Type.BlockableBy.Target
import coup.game.Influence.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.min

sealed class Action(val type: Type) {
  abstract suspend fun perform(deck: Deck)
  fun canBeBlockedBy(player: Player): Boolean = when (type.blockableBy) {
    NoOne -> false
    Target -> target == player
    Anyone -> true
  }

  abstract val player: Player
  open val target: Player? = null
  val incontestable by type::incontestable

  @Serializable
  enum class Type {
    Income,
    @SerialName("Foreign Aid")
    ForeignAid {
      override val blockingInfluences = setOf(Duke)
      override val blockableBy = Anyone
    },
    Tax {
      override val neededInfluence = Duke
    },
    Steal {
      override val neededInfluence = Captain
      override val blockableBy = Target
      override val blockingInfluences = setOf(Captain, Ambassador)
      override val hasTarget = true
    },
    Exchange {
      override val neededInfluence = Ambassador
    },
    Assassinate {
      override val neededInfluence = Assassin
      override val blockableBy = Target
      override val blockingInfluences = setOf(Contessa)
      override val cost = 3
      override val hasTarget = true
    },
    Coup {
      override val cost = 7
      override val hasTarget = true
    };

    open val cost: Int = 0
    open val hasTarget: Boolean = false
    open val neededInfluence: Influence? = null
    open val blockingInfluences: Set<Influence> = emptySet()
    enum class BlockableBy { NoOne, Target, Anyone }
    open val blockableBy = NoOne
    val incontestable get() = neededInfluence == null && blockableBy == NoOne
  }

  companion object {
    fun create(type: Type, player: Player, target: Player?): Action = when (type) {
      Type.Income -> Income(player)
      Type.ForeignAid -> ForeignAid(player)
      Type.Tax -> Tax(player)
      Type.Steal -> Steal(player, target!!)
      Type.Exchange -> Exchange(player)
      Type.Assassinate -> Assassinate(player, target!!)
      Type.Coup -> Coup(player, target!!)
    }
  }

  data class Income(override val player: Player) : Action(Type.Income) {
    override suspend fun perform(deck: Deck) = player.gainIsk(1)
  }

  data class ForeignAid(override val player: Player) : Action(Type.ForeignAid) {
    override suspend fun perform(deck: Deck) = player.gainIsk(2)
  }

  data class Tax(override val player: Player) : Action(Type.Tax) {
    override suspend fun perform(deck: Deck) = player.gainIsk(3)
  }

  data class Steal(override val player: Player, override val target: Player) : Action(Type.Steal) {
    override suspend fun perform(deck: Deck) {
      val amount = min(target.isk, 2)
      target.loseIsk(amount)
      player.gainIsk(amount)
    }
  }

  data class Exchange(override val player: Player) : Action(Type.Exchange) {
    override suspend fun perform(deck: Deck) = player.exchangeWith(deck)
  }

  data class Assassinate(override val player: Player, override val target: Player) : Action(Type.Assassinate) {
    override suspend fun perform(deck: Deck) {
      player.loseIsk(3)
      target.loseInfluence()
    }
  }

  data class Coup(override val player: Player, override val target: Player) : Action(Type.Coup) {
    override suspend fun perform(deck: Deck) {
      player.loseIsk(7)
      target.loseInfluence()
    }
  }
}