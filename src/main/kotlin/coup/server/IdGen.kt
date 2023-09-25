package coup.server

import io.ktor.util.*
import java.awt.Color
import kotlin.experimental.xor

private val alphabet = ('A'..'Z') + ('a'..'z') + ('0'..'9')

fun newId(length: Int) = (1..length).map { alphabet.random() }
  .joinToString(separator = "")

val newId: String get() = newId(8)

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