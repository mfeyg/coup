package coup.server

import io.ktor.util.*
import java.awt.Color

private val alphabet = ('A'..'Z') + ('a'..'z') + ('0'..'9')

fun newId(length: Int) = (1..length).map { alphabet.random() }
  .joinTo(StringBuilder(), separator = "").toString()

val newId: String get() = newId(8)

suspend fun idColor(id: String): Color {
  val digest = Digest("SHA-256")
  digest += id.toByteArray()
  var bytes = listOf(0, 0, 0)
  digest.build().asSequence().chunked(3).forEach { digs ->
    bytes = bytes.zip(digs + 0 + 0) { a, b -> a xor b.toInt() }
  }
  val (x, y, z) = bytes
  return Color.getHSBColor(x / 255f, y / 255f, z / 255f)
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