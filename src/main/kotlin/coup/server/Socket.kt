package coup.server

import coup.server.message.CancelGameStart.send
import coup.server.message.Id
import coup.server.message.Message
import coup.server.prompt.Prompt
import io.ktor.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Socket {
  val id = newId
  var name: String? = null

  private val outgoing = Channel<Frame>(UNLIMITED)
  private val awaiting = mutableMapOf<String, MutableSharedFlow<String>>()
  private val activePrompts = mutableSetOf<Prompt<*>>()
  private val messageFlow = MutableSharedFlow<Message>(extraBufferCapacity = UNLIMITED)
  private val mutex = Mutex()

  val messages: Flow<Message> = messageFlow.asSharedFlow()
  val state = MutableStateFlow<Message?>(null)

  suspend fun <T> prompt(prompt: Prompt<T>): T {
    val responseFlow = MutableSharedFlow<String>()
    mutex.withLock {
      activePrompts.add(prompt)
      awaiting[prompt.id] = responseFlow
    }
    outgoing.send(prompt.requestFrame)
    val response = responseFlow.first()
    mutex.withLock {
      activePrompts.remove(prompt)
      awaiting.remove(prompt.id)
    }
    return prompt.readResponse(response)
  }

  suspend fun send(message: Message) {
    outgoing.send(message.toFrame())
  }

  private suspend fun receiveFrame(frame: Frame) {
    val text = (frame as Frame.Text).readText()
    val promptResponsePattern = Regex("\\[(\\w+)](\\{.*})")
    when (val match = promptResponsePattern.matchEntire(text)) {
      is MatchResult -> {
        val (_, id, response) = match.groupValues
        val flow = mutex.withLock { awaiting[id] }
        flow?.emit(response)
      }

      else -> {
        messageFlow.emit(
          Message.read(text)
            ?: throw ServerError("Could not read message $text")
        )
      }
    }
  }

  init {
    outgoing.trySend(Id(id).toFrame())
  }

  suspend fun connect(session: WebSocketSession) = coroutineScope {
    val sendJob = launch {
      state.onEach { it?.let { session.send(it) } }.launchIn(this)
      mutex.withLock {
        for (prompt in activePrompts) {
          session.outgoing.send(prompt.requestFrame)
        }
      }
      for (frame in outgoing) {
        session.outgoing.send(frame)
      }
    }
    session.incoming.consumeEach { receiveFrame(it) }
    sendJob.cancelAndJoin()
  }
}