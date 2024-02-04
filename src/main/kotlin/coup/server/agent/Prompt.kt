package coup.server.agent

import coup.server.Id
import kotlinx.coroutines.flow.Flow

/** A prompt to the user with a typed response. */
interface Prompt<T> {
  val id: Id

  /** The message to display to the user. */
  val message: Flow<String>

  /** Registers a response from the user. */
  fun respond(response: String)

  /** Returns the user response when it's available. */
  suspend fun response(): T
}
