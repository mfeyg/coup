package coup.server

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/** A restartable task that manages its own scope. */
class DynamicTask(
  private val context: CoroutineContext = Dispatchers.Default,
  private val task: CoroutineScope.() -> Unit,
) {
  private var scope: CoroutineScope? = null
  private val mutex = Mutex()
  private var activeInstances = 0

  private fun freshScope(): CoroutineScope {
    val scope = CoroutineScope(context)
    task(scope)
    return scope
  }

  suspend fun runWhile(block: suspend () -> Unit) {
    try {
      mutex.withLock {
        activeInstances++
        scope = scope?.takeIf { it.isActive } ?: freshScope()
      }
      block()
    } finally {
      mutex.withLock {
        if (--activeInstances == 0) scope?.cancel()
      }
    }
  }
}