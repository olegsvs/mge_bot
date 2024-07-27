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
    val singleplayerTime: SingleplayerTime? = null,
)

@Serializable
data class SingleplayerTime(
    val mainStory: AllPlaystyles? = null,
    val extras: AllPlaystyles? = null,
    val completionist: AllPlaystyles? = null,
    val speedrunTime: AllPlaystyles? = null,
    val allPlaystyles: AllPlaystyles? = null,
) {
    val firstExistsTime: Pair<String, AllPlaystyles>
        get() {
            if (mainStory != null) {
                return Pair("main story", mainStory)
            } else if (extras != null) {
                return Pair("main+extras", extras)
            } else if (completionist != null) {
                return Pair("completionist", completionist)
            } else if (speedrunTime != null) {
                return Pair("speedrunTime", speedrunTime)
            } else if (allPlaystyles != null) {
                return Pair("allPlaystyles", allPlaystyles)
            } else {
                return Pair("Записей не найдено", AllPlaystyles(0L,0L,0L))
            }
        }
}

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