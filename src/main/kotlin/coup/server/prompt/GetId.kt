package coup.server.prompt

import kotlinx.serialization.Serializable

data object GetId : Prompt<String?>() {
  @Serializable
  data class Response(val id: String?)

  override fun prompt() = sendAndReceive { (id): Response -> id }
}
