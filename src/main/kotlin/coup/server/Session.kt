package coup.server

import coup.server.message.Message
import coup.server.prompt.Promptable
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.logging.Level
import java.util.logging.Logger

/** Represents a user's session. */
class Session<State>(
  val id: String,
  var name: String,
  private val stateSerializer: KSerializer<State>,
) : Promptable {
  private val activePrompts = MutableStateFlow(mapOf<String, String>())
  private val activeListeners = MutableStateFlow(mapOf<String, CompletableDeferred<String>>())
  private val incomingMessages = MutableSharedFlow<Message>(replay = UNLIMITED)
  private val events = MutableSharedFlow<String>(replay = UNLIMITED)
  private val state = MutableStateFlow<State?>(null)
  private val connectionCount = MutableStateFlow(0)

  val messages get() = incomingMessages.asSharedFlow()
  val connections get() = connectionCount.asStateFlow()

  @Serializable
  private data class Prompt<T>(val type: String, val id: String, val prompt: T)

  override suspend fun <T, RequestT, ResponseT> prompt(
    promptType: String,
    request: RequestT,
    requestSerializer: KSerializer<RequestT>,
    responseSerializer: KSerializer<ResponseT>,
    readResponse: (ResponseT) -> T
  ): T {
    while (true) {
      val response = CompletableDeferred<String>()
      val id = newId
      val prompt = Json.encodeToString(Prompt.serializer(requestSerializer), Prompt(promptType, id, request))
      activePrompts.update { it + (id to prompt) }
      activeListeners.update { it + (id to response) }
      try {
        return readResponse(Json.decodeFromString(responseSerializer, response.await()))
      } catch (e: IllegalArgumentException) {
        Logger.getGlobal().log(Level.WARNING, e) { "Failed to read response" }
      } finally {
        activePrompts.update { it - id }
        activeListeners.update { it - id }
      }
    }
  }

  suspend fun <T> event(event: T, type: String, serializer: KSerializer<T>) {
    events.emit(type + Json.encodeToString(serializer, event))
  }

  suspend inline fun <reified T> event(event: T) = event(event, T::class.simpleName!!, serializer())

  fun setState(newState: State) {
    state.value = newState
  }

  inline fun <reified T> newSession(): Session<T> = Session(id, name, serializer())

  private suspend fun receiveFrame(frame: Frame) {
    val text = (frame as Frame.Text).readText()
    val promptResponsePattern = Regex("\\[(\\w+)](\\{.*})")
    when (val match = promptResponsePattern.matchEntire(text)) {
      is MatchResult -> {
        val (_, id, response) = match.groupValues
        activeListeners.value[id]?.complete(response)
      }

      else -> {
        incomingMessages.emit(
          Message.read(text)
            ?: throw ServerError("Could not read message $text")
        )
      }
    }
  }

  suspend fun connect(connection: WebSocketSession) = coroutineScope {
    connectionCount.update { it + 1 }
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
      connectionCount.update { it - 1 }
    }
  }

  companion object {
    inline operator fun <reified T> invoke(id: String, name: String): Session<T> = Session(id, name, serializer())
  }
}