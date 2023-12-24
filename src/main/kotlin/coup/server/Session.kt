package coup.server

import coup.server.ConnectionController.SocketConnection
import coup.server.prompt.Promptable
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** Represents a user's session. */
class Session<State>(
  val id: String,
  var name: String,
  private val stateSerializer: KSerializer<State>,
) : Promptable {
  private val activePrompts = MutableStateFlow(mapOf<String, String>())
  private val activeListeners = MutableStateFlow(mapOf<String, (String) -> Unit>())
  private val incomingMessages = MutableSharedFlow<String>(replay = UNLIMITED)
  private val events = MutableSharedFlow<String>(replay = UNLIMITED)
  private val state = MutableStateFlow<State?>(null)
  private val connections = MutableStateFlow(setOf<SocketConnection>())
  val connectionCount = connections.value.size

  val messages get() = incomingMessages.asSharedFlow()

  @Serializable
  private data class Prompt<T>(val type: String, val id: String, val prompt: T)

  override suspend fun <T, RequestT, ResponseT> prompt(
    promptType: String,
    request: RequestT,
    requestSerializer: KSerializer<RequestT>,
    responseSerializer: KSerializer<ResponseT>,
    readResponse: (ResponseT) -> T
  ): T {
    val response = CompletableDeferred<ResponseT>()
    val id = newId
    val prompt = Json.encodeToString(Prompt.serializer(requestSerializer), Prompt(promptType, id, request))
    try {
      activePrompts.update { it + (id to prompt) }
      activeListeners.update { it + (id to { response.complete(Json.decodeFromString(responseSerializer, it)) }) }
      return readResponse(response.await())
    } finally {
      activePrompts.update { it - id }
      activeListeners.update { it - id }
    }
  }

  suspend fun event(event: String) = events.emit(event)

  fun setState(newState: State) {
    state.value = newState
  }

  private suspend fun receiveFrame(frame: Frame) {
    val text = (frame as Frame.Text).readText()
    val promptResponsePattern = Regex("\\[(\\w+)](\\{.*})")
    when (val match = promptResponsePattern.matchEntire(text)) {
      is MatchResult -> {
        val (_, id, response) = match.groupValues
        activeListeners.value[id]?.invoke(response)
      }

      else -> incomingMessages.emit(text)
    }
  }

  suspend fun connect(connection: SocketConnection) = coroutineScope {
    connections.update { it + connection }
    try {
      val listeningJob = launch {
        state.onEach { state ->
          state?.let {
            connection.send("State" + Json.encodeToString(stateSerializer, state))
          }
        }.launchIn(this)
        events.onEach { connection.send(it) }.launchIn(this)
        activePrompts.onEach { prompts ->
          connection.send("Prompts[" + prompts.values.joinToString(",") + "]")
        }.launchIn(this)
      }
      for (frame in connection.incoming) {
        receiveFrame(frame)
      }
      listeningJob.cancelAndJoin()
    } finally {
      connections.update { it - connection }
    }
  }

  fun disconnect() {
    connections.value.forEach { connection ->
      connection.cancel()
      connections.update { it - connection }
    }
  }

  companion object {
    inline operator fun <reified T> invoke(id: String, name: String): Session<T> = Session(id, name, serializer())

    inline operator fun <reified T> invoke(session: Session<*>): Session<T> = Session(session.id, session.name)

    inline operator fun <reified T> invoke(connection: SocketConnection) = Session<T>(connection.id, connection.name)
  }
}