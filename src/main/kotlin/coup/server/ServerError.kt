package coup.server

open class ServerError(message: String? = null) : Exception(message?.let { "Server error: $message" })