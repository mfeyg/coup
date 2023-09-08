package coup.server

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

class SocketController {
  private val sockets = WeakHashMap<Socket, Mutex>()
  private val mutex = Mutex()

  @Serializable
  data class IdResponse(val id: String?)

  suspend fun withSocket(session: WebSocketSession, block: suspend (Socket) -> Unit) {
    session.send("GetId")
    val (id) = Json.decodeFromString(
      IdResponse.serializer(), (session.incoming.receive() as Frame.Text).readText())
    val (socket, mutex) = mutex.withLock {
      val socket = sockets.keys.find { it.id == id } ?: Socket()
      val mutex = sockets.getOrPut(socket) { Mutex() }
      socket to mutex
    }
    mutex.withLock { block(socket) }
  }
}