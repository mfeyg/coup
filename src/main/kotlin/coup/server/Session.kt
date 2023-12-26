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
import kotlin.time.Duration.Companion.seconds

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
  val connectionCount = connections.value.size

  val messages get() = incomingMessages.asSharedFlow()

  private val prompts = MutableStateFlow(mapOf<String, Prompt<*>>())

  class Prompt<T>(
    val id: String,
    prompt: (timeout: Int?) -> String,
    private val readResponse: (String) -> T,
    private val timeoutOption: TimeoutOption<T>?,
  ) {
    data class TimeoutOption<T>(val timeout: Int, val defaultValue: T)

    private val timeout = MutableStateFlow(timeoutOption?.timeout)
    private val response = CompletableDeferred<T>()
    val prompt = this.timeout.map(prompt)

    fun complete(value: String) {
      response.complete(readResponse(value))
    }

    suspend fun await(): T = coroutineScope {
      launch timeout@{
        val (_, defaultValue) = timeoutOption ?: return@timeout
        timeout.collectLatest timeouts@{ value ->
          if (value == null) return@timeouts
          if (value == 0) response.complete(defaultValue)
          else {
            delay(1.seconds)
            timeout.value = value - 1
          }
        }
      }.let { job -> launch { response.join(); job.cancel() } }
      response.await()
    }
  }

  class PromptBuilder<T> {
    @Serializable
    private data class PromptRequest<T>(
      val type: String? = null,
      val id: String,
      val prompt: T? = null,
      val timeout: Int? = null
    )

    var type: String? = null
    var readResponse: ((String) -> T)? = null
    private val id = newId
    private var prompt: (timer: Int?) -> String = { Json.encodeToString(PromptRequest(type, id, null, it)) }

    private var timeoutOption: Prompt.TimeoutOption<T>? = null

    fun <RequestT> request(request: RequestT, serializer: KSerializer<RequestT>) {
      prompt = { Json.encodeToString(PromptRequest.serializer(serializer), PromptRequest(type, id, request, it)) }
    }

    inline fun <reified T> request(request: T) = request(request, serializer())

    inline fun <reified ResponseT> readResponse(noinline read: (ResponseT) -> T) {
      readResponse = { read(Json.decodeFromString(it)) }
    }

    fun timeout(timeout: Int?, defaultValue: () -> T) {
      timeoutOption = timeout?.let { Prompt.TimeoutOption(timeout, defaultValue()) }
    }

    fun prompt() = Prompt(id, prompt, readResponse!!, timeoutOption)
  }

  suspend fun <T> prompt(build: PromptBuilder<T>.() -> Unit): T {
    val prompt = PromptBuilder<T>().also(build).prompt()
    prompts.update { it + (prompt.id to prompt) }
    try {
      return prompt.await()
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
        prompts.value[id]?.complete(response)
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