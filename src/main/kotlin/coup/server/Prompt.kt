package coup.server

import kotlinx.coroutines.flow.Flow

interface Prompt<T> {
  val id: Id
  val prompt: Flow<String>
  fun respond(response: String)
  suspend fun response(): T
}
