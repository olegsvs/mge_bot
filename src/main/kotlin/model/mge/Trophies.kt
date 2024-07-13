package model.mge

import kotlinx.serialization.Serializable

@Serializable
data class Trophies(
    val trophies: List<Trophy>
) {
    override fun toString(): String {
        return "Trophies(trophies=$trophies)"
    }
}

@Serializable
data class Trophy(
    val keyName: String,
    val name: String,
    val leaderboard: LeaderBoard,
) {
    override fun toString(): String {
        return "${leaderboard.title.replace("\n"," ")}\n"
    }
}

@Serializable
data class LeaderBoard(
    val title: String,
    val list: List<Leader> = listOf(),
) {
    override fun toString(): String {
        var leaders = ""
        for(leader in list) {
            leaders += "$leader "
        }

        return leaders.trim()
    }
}

@Serializable
data class Leader(
    val playerID: String,
    val playerName: String,
    val trophyScore: String,
    val placementScore: String?,
) {
    override fun toString(): String {
        return "$playerNameFormatted, очки: $trophyScore$placementScoreFormatted"
    }

    private val placementScoreFormatted: String
        get() = if(placementScore == null || placementScore == "0" || placementScore == "null") "" else " (+${placementScore})"

    private val playerNameFormatted: String
        get() = if(playerName == "???") "Неизвестно" else playerName
}