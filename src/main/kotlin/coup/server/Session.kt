package coup.server

import coup.server.message.Event
import coup.server.message.Event.Companion.send
import coup.server.message.Message
import coup.server.message.StartGame.send
import coup.server.prompt.GetId.send
import coup.server.prompt.Prompt
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*

/** Represents a user's session. */
class Session<State : Message>(
  val id: String,
  var name: String,
  initialState: State,
) {
  private val activePrompts = MutableStateFlow(mapOf<String, Pair<Prompt<*>, CompletableDeferred<String>>>())
  private val incomingMessages = MutableSharedFlow<Message>(replay = UNLIMITED)
  private val events = MutableSharedFlow<Event>(replay = UNLIMITED)
  private val state = MutableStateFlow(initialState)
  private val activeConnections = MutableStateFlow(listOf<WebSocketSession>())

  val messages get() = incomingMessages.asSharedFlow()
  val active get() = activeConnections.map { it.isNotEmpty() }

  suspend fun <T> prompt(prompt: Prompt<T>): T {
    val response = CompletableDeferred<String>()
    activePrompts.update {
      it + (prompt.id to (prompt to response))
    }
    val responseValue = response.await()
    activePrompts.update { it - prompt.id }
    return prompt.readResponse(responseValue)
  }

  suspend fun event(event: Event) {
    events.emit(event)
  }

  fun setState(newState: State) {
    state.value = newState
  }

  private suspend fun receiveFrame(frame: Frame) {
    val text = (frame as Frame.Text).readText()
    val promptResponsePattern = Regex("\\[(\\w+)](\\{.*})")
    when (val match = promptResponsePattern.matchEntire(text)) {
      is MatchResult -> {
        val (_, id, response) = match.groupValues
        val deferred = activePrompts.value[id]?.second
        deferred?.complete(response)
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
    val listeningJob = launch {
      state.onEach { connection.send(it) }.launchIn(this)
      events.onEach { connection.send(it) }.launchIn(this)
      val sentPrompts = mutableSetOf<String>()
      activePrompts.onEach { prompts ->
        (prompts - sentPrompts).forEach { (id, value) ->
          val (prompt, _) = value
          connection.send(prompt)
          sentPrompts += id
        }
      }.launchIn(this)
    }
    activeConnections.update { it + connection }
    try {
      for (frame in connection.incoming) {
        receiveFrame(frame)
      }
    } finally {
      activeConnections.update { it - connection }
      listeningJob.cancelAndJoin()
    }
  }
}