package coup.server

import kotlinx.serialization.Serializable

@Serializable
data class Options(val responseTimer: Int? = null)