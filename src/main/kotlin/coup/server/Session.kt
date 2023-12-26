package coup.server

import coup.server.ConnectionController.SocketConnection
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
  private val activePrompts = MutableStateFlow(mapOf<String, String>())
  private val activeListeners = MutableStateFlow(mapOf<String, (String) -> Unit>())
  private val incomingMessages = MutableSharedFlow<Message>()
  private val events = MutableSharedFlow<String>(replay = UNLIMITED)
  private val connections = MutableStateFlow(setOf<SocketConnection>())
  val connectionCount = connections.value.size

  val messages get() = incomingMessages.asSharedFlow()

  @Serializable
  data class PromptRequest<T>(val type: String, val id: String, val prompt: T)

  inner class PromptBuilder<T>(private val type: String, private val readResponse: (String) -> T) {

    private val id = newId
    private var prompt = Json.encodeToString(PromptRequest(type, id, null))

    fun <RequestT> request(request: RequestT, serializer: KSerializer<RequestT>): PromptBuilder<T> {
      prompt = Json.encodeToString(PromptRequest.serializer(serializer), PromptRequest(type, id, request))
      return this
    }

    inline fun <reified T> request(request: T) = request(request, serializer())

    private val response = CompletableDeferred<T>()

    suspend fun send(): T {
      try {
        activePrompts.update { it + (id to prompt) }
        activeListeners.update { it + (id to { response.complete(readResponse(it)) }) }
        return response.await()
      } finally {
        activePrompts.update { it - id }
        activeListeners.update { it - id }
      }
    }
  }

  @JvmName("promptHelper")
  fun <T> prompt(type: String, readResponse: (String) -> T) = PromptBuilder(type, readResponse)

  inline fun <T, reified Response> prompt(type: String, noinline readResponse: (Response) -> T) =
    prompt(type) { readResponse(Json.decodeFromString(it)) }

  suspend fun event(event: String) = events.emit(event)

  private suspend fun receiveFrame(frame: Frame) {
    val text = (frame as Frame.Text).readText()
    val promptResponsePattern = Regex("""\[(\w+)](\{.*})""")
    when (val match = promptResponsePattern.matchEntire(text)) {
      is MatchResult -> {
        val (_, id, response) = match.groupValues
        activeListeners.value[id]?.invoke(response)
      }

      else -> incomingMessages.emit(messageParser(text))
    }
  }

  suspend fun connect(connection: SocketConnection) = coroutineScope {
    connections.update { it + connection }
    try {
      val listeningJob = launch {
        state.onEach { state ->
          connection.send("State:" + Json.encodeToString(stateSerializer, state))
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