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
  private val baseGame: Game,
  private val players: List<Player>,
  private val playerColor: (Player) -> String,
  private val connectPlayer: suspend (SocketConnection) -> Unit?,
  private val setWinner: (Player) -> Unit,
) {
  private val observers = MutableStateFlow(mapOf<String, Session<GameState, Nothing>>())
  private val scope = CoroutineScope(Dispatchers.Default)
  private val connectionCount = MutableStateFlow(0)

  private val _onShutDown = MutableStateFlow(listOf<() -> Unit>())
  fun onShutDown(block: () -> Unit) = _onShutDown.update { it + block }

  private fun state(player: Player? = null) =
    combine(players.map { it.updates } + baseGame.updates) {
      GameState(
        players = players,
        thisPlayer = player,
        playerColor = playerColor,
        currentPlayer = baseGame.currentPlayer,
        winner = baseGame.winner,
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
      baseGame.play()
      baseGame.winner?.let { setWinner(it) }
    }
  }

  companion object {
    suspend operator fun invoke(
      playerIds: List<String>,
      playerNames: List<String>,
      lobby: Lobby,
      ruleset: Ruleset,
      options: GameOptions,
    ): GameServer {

      val numPlayers = playerIds.size
      val playerSessions = object {
        lateinit var value: List<Session<GameState, Nothing>>
      }

      fun agent(player: Player) =
        PromptContext(player, ruleset, options, object : PromptContext.Perform {
          override suspend fun <T> invoke(prompt: Prompt<T>) =
            playerSessions.value[player.playerNumber].prompt(prompt)
        }).agent()

      val playerNumberById = buildMap {
        playerIds.forEachIndexed { index, id ->
          put(id, index)
        }
      }

      fun session(id: String) = playerNumberById[id]?.let { playerSessions.value[it] }

      val players: List<Player> = List(numPlayers) { playerNumber ->
        Player(playerNames[playerNumber], playerNumber, ruleset, ::agent)
      }
      val playerColors: List<String> = playerIds.map { idColor(it).cssColor }

      val gameServer = GameServer(
        Game(ruleset, players),
        players,
        { playerColors[it.playerNumber] },
        { session(it.id)?.connect(it) },
        { lobby.setChampion(playerIds[it.playerNumber]) },
      )

      playerSessions.value = List(numPlayers) { playerNumber ->
        Session(
          playerIds[playerNumber],
          playerNames[playerNumber],
          gameServer.state(players[playerNumber])
        )
      }
      return gameServer
    }
  }

  suspend fun connect(connection: SocketConnection) {
    try {
      connectionCount.update { it + 1 }
      connectPlayer(connection)
        ?: observingSession(connection.id, connection.name).connect(connection)
    } finally {
      connectionCount.update { it - 1 }
    }
  }

  private fun observingSession(id: String, observerName: String) =
    observers.updateAndGet { observers ->
      if (observers.containsKey(id)) observers else
        observers + (id to Session(id, observerName, state()))
    }.getValue(id)
}