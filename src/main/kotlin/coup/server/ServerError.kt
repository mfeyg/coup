package coup.server

open class ServerError(message: String) : Exception("Server error: $message") {

  class BadRequest(message: String? = null) :
    ServerError("Bad request" + (if (message == null) "" else ": $message"))
}