package coup.server

open class ServerError(message: String? = null) : Exception("Server error: $message") {

  class BadRequest(message: String? = null) :
    ServerError("Bad request" + (if (message == null) "" else ": $message"))
}