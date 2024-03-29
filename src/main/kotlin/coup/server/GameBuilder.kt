package coup.server

import coup.game.Game
import coup.game.Player
import coup.game.rules.Ruleset
import coup.game.rules.StandardRules
import coup.server.agent.ComputerAgent
import coup.server.agent.PlayerAgent

class GameBuilder {
  private val players = mutableListOf<Pair<Person, (number: Int, GameServer) -> Player>>()
  private val people get() = players.map { it.first }
  var ruleset: Ruleset = StandardRules()
  var options: GameOptions = GameOptions.default
  private var numberOfComputerPlayers = 0

  fun addHumanPlayer(person: Person) {
    players.add(person to { number, server ->
      Player(number, person.name, ruleset) { player ->
        PlayerAgent(player, ruleset, people, options, server.session(person))
      }
    })
  }

  fun addComputerPlayer() {
    val id = Id()
    val name = "Computer ${++numberOfComputerPlayers}"
    val color = randomColor()
    val computer = Person(id, name, color)
    players.add(computer to { number, _ ->
      Player(number, name, ruleset) { player ->
        ComputerAgent(ruleset, player)
      }
    })
  }

  fun shufflePlayers() = players.shuffle()

  private fun randomColor() = "#" + buildString {
    repeat(6) {
      append((('0'..'9') + ('a'..'f')).random())
    }
  }

  companion object {
    operator fun invoke(build: GameBuilder.() -> Unit): GameServer {
      val config = GameBuilder().also(build)
      val board =
        { server: GameServer -> config.ruleset.setUpBoard(config.players.mapIndexed { i, it -> it.second(i, server) }) }
      return GameServer(config.people) { Game(config.ruleset, board(it)) }
    }
  }
}