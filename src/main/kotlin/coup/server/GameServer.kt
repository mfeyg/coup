package coup.server

import coup.game.*
import coup.game.Game
import coup.game.rules.Ruleset
import coup.server.ConnectionController.SocketConnection
import coup.server.agent.*
import coup.server.agent.PlayerAgent.Companion.agent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.hours

class GameServer private constructor(
  private val game: Game,
  private val players: List<Person>,
  private val connectPlayer: suspend (SocketConnection) -> Unit?,
  private val onComplete: (Game) -> Unit,
) {
  private val observers = MutableStateFlow(mapOf<String, Session<GameState, Nothing>>())
  private val scope = CoroutineScope(Dispatchers.Default)
  private val connectionCount = MutableStateFlow(0)

  private val _onShutDown = MutableStateFlow(listOf<() -> Unit>())
  fun onShutDown(block: () -> Unit) = _onShutDown.update { it + block }

  private fun state(player: Player? = null) =
    game.updates.map {
      GameState(
        player = player?.number?.let { i -> GameState.Player(players[i], game.players[i]) },
        players = players.indices.map { i -> GameState.Opponent(players[i], game.players[i]) },
        currentTurn = game.currentPlayer.number.takeIf { game.winner == null },
        winner = game.winner?.number,
      )
    }

  fun start() {
    scope.launch {
      connectionCount.collectLatest { connections ->
        if (connections == 0) {
          delay(1.hours)
          _onShutDown.value.forEach { it() }
          scope.cancel()
        }
      }
    }
    scope.launch {
      game.play()
      onComplete(game)
    }
  }

  companion object {
    suspend operator fun invoke(
      players: List<Person>,
      lobby: Lobby,
      ruleset: Ruleset,
      options: GameOptions,
    ): GameServer {

      val playerSessions = object {
        lateinit var value: List<Session<GameState, Nothing>>
      }

      fun agent(player: Player) =
        PromptContext(player, ruleset, players, options, object : PromptContext.Perform {
          override suspend fun <T> invoke(prompt: Prompt<T>) =
            playerSessions.value[player.number].prompt(prompt)
        }).agent()

      val playerNumberById = buildMap {
        players.forEachIndexed { index, it ->
          put(it.id, index)
        }
      }

      fun session(id: String) = playerNumberById[id]?.let { playerSessions.value[it] }

      val gamePlayers: List<Player> = List(players.size) { playerNumber ->
        Player(playerNumber, ruleset, ::agent)
      }

      val gameServer = GameServer(
        Game(ruleset, players.size, ::agent),
        players,
        { session(it.user.id)?.connect(it) },
        { game -> game.winner?.let { lobby.setChampion(players[it.number].id) } },
      )

      playerSessions.value = List(gamePlayers.size) { playerNumber ->
        Session(
          players[playerNumber],
          gameServer.state(gamePlayers[playerNumber])
        )
      }
      return gameServer
    }
  }

  suspend fun connect(connection: SocketConnection) {
    try {
      connectionCount.update { it + 1 }
      connectPlayer(connection)
        ?: observingSession(connection.user).connect(connection)
    } finally {
      connectionCount.update { it - 1 }
    }
  }

  private fun observingSession(user: Person) =
    observers.updateAndGet { observers ->
      if (observers.containsKey(user.id)) observers else
        observers + (user.id to Session(user, state()))
    }.getValue(user.id)
}