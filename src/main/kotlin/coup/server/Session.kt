package coup.server

import coup.server.Sendable.Companion.send
import coup.server.message.Message
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*

/** Represents a user's session. */
class Session<State : Any>(
  val id: String,
  var name: String,
) : Prompt {
  private val activePrompts = MutableStateFlow(mapOf<String, Pair<String, CompletableDeferred<String>>>())
  private val incomingMessages = MutableSharedFlow<Message>(replay = UNLIMITED)
  private val events = MutableSharedFlow<Sendable>(replay = UNLIMITED)
  private val state = MutableStateFlow<State?>(null)

  val messages get() = incomingMessages.asSharedFlow()

  override suspend fun <T> prompt(promptType: String, request: String, readResponse: (String) -> T): T {
    val response = CompletableDeferred<String>()
    val id = newId
    activePrompts.update {
      it + (id to ("$promptType[$id]$request" to response))
    }
    val responseValue = response.await()
    activePrompts.update { it - id }
    return readResponse(responseValue)
  }

  suspend fun event(event: Sendable) {
    events.emit(event)
  }

  fun setState(newState: State) {
    state.value = newState
  }

  fun <T : Any> newSession(): Session<T> = Session(id, name)

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
      state.onEach { it?.let { connection.send(StateUpdate(it)) } }.launchIn(this)
      events.onEach { connection.send(it) }.launchIn(this)
      val sentPrompts = mutableSetOf<String>()
      activePrompts.onEach { prompts ->
        (prompts - sentPrompts).forEach { (id, value) ->
          val (prompt, _) = value
          connection.send(Frame.Text(prompt))
          sentPrompts += id
        }
      }.launchIn(this)
    }
    for (frame in connection.incoming) {
      receiveFrame(frame)
    }
    listeningJob.cancelAndJoin()
  }
}