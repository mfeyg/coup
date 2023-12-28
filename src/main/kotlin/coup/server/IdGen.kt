package coup.server

data class Id(val value: String)

private val alphabet = ('A'..'Z') + ('a'..'z') + ('0'..'9')

fun newId(length: Int) = Id((1..length).map { alphabet.random() }
  .joinToString(separator = ""))

val newId: Id get() = newId(8)