@file:Suppress("SpellCheckingInspection")

package model.hltb

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class HLTBSearchResponse(
    val gameName: String,
    val gameId: Int,
)

@Serializable
data class HLTBGameResponse(
    val title: String,
    val singleplayerTime: SingleplayerTime?,
)

@Serializable
data class SingleplayerTime(
    val mainStory: AllPlaystyles,
)

@Serializable
data class AllPlaystyles(
    val polled: Long,
    val averageSec: Long,
    val rushedSec: Long,
) {
    val averageSecFormatted: String
        get() {
            val hours = averageSec / 3600
            val minutes = (averageSec % 3600) / 60
            return "${hours}ч${minutes}м"
        }

    val rushedSecFormatted: String
        get() {
            val hours = rushedSec / 3600
            val minutes = (rushedSec % 3600) / 60
            return "${hours}ч${minutes}м"
        }

    override fun toString(): String {
        return "AVG:$averageSecFormatted || Rushed:$rushedSecFormatted || Polled: $polled"
    }

}