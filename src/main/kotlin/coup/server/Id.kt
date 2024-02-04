package coup.server

import java.security.SecureRandom

data class Id(val value: String) {
  companion object {
    private val alphabet = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    private val random = SecureRandom()

    operator fun invoke(length: Int = 8): Id {
      return Id((1..length)
        .map { alphabet[random.nextInt(alphabet.size)] }
        .joinToString(separator = ""))
    }
  }
}
