package coup.server

import kotlinx.serialization.Serializable

@Serializable
data class GameOptions(val responseTimer: Int?) {
  companion object {
    val default = GameOptions(responseTimer = 30)
  }
}