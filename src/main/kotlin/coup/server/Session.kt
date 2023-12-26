package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** Represents a user's session. */
class Session<State, Message>(
  val id: String,
  var name: String,
  private val state: Flow<State>,
  private val stateSerializer: KSerializer<State>,
  private val messageParser: (String) -> Message = { throw IllegalArgumentException("Unexpected message $it") },
) {
  private val incomingMessages = MutableSharedFlow<Message>()
  private val events = MutableSharedFlow<String>(replay = UNLIMITED)
  private val connections = MutableStateFlow(setOf<SocketConnection>())
  val connectionCount get() = connections.value.size

  val messages get() = incomingMessages.asSharedFlow()

  private val prompts = MutableStateFlow(mapOf<String, Prompt<*>>())

  suspend fun <T> prompt(prompt: Prompt<T>): T {
    try {
      prompts.update { it + (prompt.id to prompt) }
      return prompt.response()
    } finally {
      prompts.update { it - prompt.id }
    }
  }

  suspend fun event(event: String) = events.emit(event)

  private suspend fun receiveFrame(frame: Frame) {
    val text = (frame as Frame.Text).readText()
    val promptResponsePattern = Regex("""\[(\w+)](\{.*})""")
    when (val match = promptResponsePattern.matchEntire(text)) {
      is MatchResult -> {
        val (_, id, response) = match.groupValues
        prompts.value[id]?.respond(response)
      }

      else -> incomingMessages.emit(messageParser(text))
    }
  }

  suspend fun connect(connection: SocketConnection) {
    connections.update { it + connection }
    try {
      coroutineScope {
        launch {
          state.onEach { state ->
            connection.send("State:" + Json.encodeToString(stateSerializer, state))
          }.launchIn(this)
          events.onEach { connection.send(it) }.launchIn(this)
          launch {
            prompts.collectLatest {
              if (it.values.isEmpty()) {
                connection.send("Prompts[]")
              }
              combine(it.values.map { it.prompt }) { prompts ->
                connection.send("Prompts[" + prompts.joinToString(",") + "]")
              }.collect()
            }
          }
          for (frame in connection.incoming) {
            receiveFrame(frame)
          }
          cancel()
        }
      }
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
    inline operator fun <reified StateT, MessageT> invoke(
      id: String,
      name: String,
      state: Flow<StateT>,
      noinline readMessage: (String) -> MessageT,
    ) = Session(id, name, state, serializer(), readMessage)

    inline operator fun <reified StateT> invoke(id: String, name: String, state: Flow<StateT>) =
      Session<StateT, Nothing>(id, name, state, serializer())
  }
}