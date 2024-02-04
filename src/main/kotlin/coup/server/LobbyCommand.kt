package coup.server

sealed class LobbyCommand {
  data object StartGame : LobbyCommand()
  data object CancelGameStart : LobbyCommand()
  data class SetResponseTimer(val responseTimer: Int?) : LobbyCommand() {
    companion object {
      val PATTERN = Regex("SetResponseTimer:(null|\\d+)")
      fun parse(input: String): SetResponseTimer {
        val (value) = PATTERN.matchEntire(input)?.destructured
          ?: throw IllegalArgumentException("Unexpected input: $input")
        return SetResponseTimer(
          when (value) {
            "null" -> null
            else -> value.toInt()
          }
        )
      }
    }
  }

  companion object {
    fun valueOf(command: String) = when {
      command == "StartGame" -> StartGame
      command == "CancelGameStart" -> CancelGameStart
      command matches SetResponseTimer.PATTERN -> SetResponseTimer.parse(command)
      else -> throw IllegalArgumentException("Unknown command $command")
    }
  }
}