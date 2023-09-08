package coup.server

private val alphabet = ('A'..'Z') + ('a'..'z') + ('0'..'9')

fun newId(length: Int) = (1..length).map { alphabet.random() }
  .joinTo(StringBuilder(), separator = "").toString()

val newId: String get() = newId(8)