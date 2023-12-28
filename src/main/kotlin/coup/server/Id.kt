package coup.server

data class Id(val value: String) {
  companion object {
    private val alphabet = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    operator fun invoke(length: Int = 8): Id {
      return Id((1..length)
        .map { alphabet.random() }
        .joinToString(separator = ""))
    }
  }
}
