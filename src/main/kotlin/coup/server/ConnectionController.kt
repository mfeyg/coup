package coup.server

import coup.server.message.CancelGameStart.send
import coup.server.message.Id
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ConnectionController {
  class SocketConnection(private val socket: WebSocketSession, val name: String, val id: String) :
    WebSocketSession by socket

  suspend fun connection(socket: WebSocketSession): SocketConnection {
    val id = readSocketId(socket) ?: newId.also { socket.send(Id(it)) }
    val name = readSocketName(socket)
    return SocketConnection(socket, name, id)
  }

  @Serializable
  private data class IdResponse(val id: String?)

  @Serializable
  private data class NameResponse(val name: String)

  private suspend fun readSocketId(socket: WebSocketSession): String? {
    socket.send("GetId")
    val (id) = Json.decodeFromString<IdResponse>(socket.receiveText())
    return id
  }

  private suspend fun readSocketName(socket: WebSocketSession): String {
    socket.send("GetName")
    val (name) = Json.decodeFromString<NameResponse>(socket.receiveText())
    return name
  }

  private suspend fun WebSocketSession.receiveText() = (incoming.receive() as Frame.Text).readText()
}