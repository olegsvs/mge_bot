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
    val placementScore: String,
) {
    override fun toString(): String {
        return "$playerName, очки: $trophyScore"
    }
}