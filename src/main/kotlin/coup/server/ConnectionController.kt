package coup.server

import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.decodeFromString
import java.awt.Color
import kotlin.experimental.xor

class ConnectionController {
  class SocketConnection(private val socket: WebSocketSession, val user: Person) :
    WebSocketSession by socket

  suspend fun connection(socket: WebSocketSession): SocketConnection {
    val id = readSocketId(socket)
      ?: newId(100).also { socket.send("Id:$it") }
    val name = readSocketName(socket)
    return SocketConnection(socket, Person(id, name, idColor(id).cssColor))
  }

  suspend fun idColor(id: String): Color {
    val digest = Digest("SHA-256")
    digest += id.toByteArray()
    val value = digest.build().reduce(Byte::xor)
    return Color.getHSBColor(value / 255f, 0.5f, 0.95f)
  }

  @OptIn(ExperimentalStdlibApi::class)
  val Color.cssColor: String
    get() {
      val colorFormat = HexFormat {
        number.prefix = "#"
        number.removeLeadingZeros = true
      }
      return (rgb and 0xffffff).toHexString(colorFormat)
    }

  @Serializable
  private data class IdResponse(val id: String?)

  @Serializable
  private data class NameResponse(val name: String)

  private suspend fun readSocketId(socket: WebSocketSession): String? {
    socket.send("GetId")
    val (id) = decodeFromString<IdResponse>(socket.receiveText())
    return id
  }

  private suspend fun readSocketName(socket: WebSocketSession): String {
    socket.send("GetName")
    val (name) = decodeFromString<NameResponse>(socket.receiveText())
    return name
  }

  private suspend fun WebSocketSession.receiveText() = (incoming.receive() as Frame.Text).readText()
}