package model.twitch

data class CoolDown(
    val channelName: String,
    val commandText: String,
    val coolDownMillis: Long,
    val lastUsageInMillis: Long,
) {}