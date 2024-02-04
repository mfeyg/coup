package coup.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/** A log which collects game events. */
class GameLog private constructor(
  private val _events: MutableStateFlow<List<Event>>,
  context: Map<String, Any> = mapOf(),
) {
  constructor() : this(_events = MutableStateFlow(listOf()))

  private val _context = AtomicReference(context)

  private val logger = LoggerFactory.getLogger(this::class.java)

  /** A log event. */
  data class Event(val eventType: String, val context: Map<String, Any>)

  /** A state flow of all the logged events. */
  val loggedEvents = _events.asStateFlow()

  class ContextBuilder(existingContext: Map<String, Any> = mapOf()) {
    private val _context = existingContext.toMutableMap()

    infix fun String.`is`(value: Any) {
      _context += this to value
    }

    fun build() = _context.toMap()
  }

  /** Logs the given event with optional additions context entries. */
  fun logEvent(eventType: String, block: ContextBuilder.() -> Unit = {}) {
    val context = ContextBuilder(_context.get()).also(block).build()
    with(logger.atInfo()) {
      context.forEach { (key, value) -> addKeyValue(key, value) }
      log(eventType)
    }
    _events.update { it + Event(eventType, context) }
  }

  /** Adds values to the log context of the current scope. */
  fun logContext(block: ContextBuilder.() -> Unit) {
    _context.updateAndGet { ContextBuilder(it).also(block).build() }
  }

  /** Adds the result of the block to the log context, if it's non-null. */
  suspend fun <T> logContext(name: String, block: suspend () -> T): T {
    val value = block()
    value?.let { _context.updateAndGet { it + (name to value) } }
    return value
  }

  /** Executes the block in a new log scope. Any added log context will be local to the log scope. */
  suspend fun logScope(block: suspend GameLog.() -> Unit) {
    GameLog(_events, _context.get()).block()
  }
}