package model.mge

import kotlinx.serialization.Serializable

@Serializable
data class Players(
    val players: List<Player>
)

@Serializable
data class Player(
    val id: String,
    val name: String,
    val avatar: Avatar,
    val level: Level,
    val dailyIncome: Double,
    val money: Double,
    val hp: HitPoints,
    val effects: List<Effect> = listOf(),
    val policeInterest: PoliceInterest,
    val combatPower: CombatPower,
    val influence: Int,
    val morale: Morale,
    val congressTokens: Int,
    val states: PlayerState,
    val characteristics: Characteristic,
    val family: Family,
    val inventory: Inventory,
    val actionPoints: ActionPoints,
    val logs: List<ActionLog> = listOf(),
    val gameLogs: List<GameLog> = listOf(),
) {
    val experience: String
        get() = "[${level.experience.current}/${level.experience.maximum}]"

    val positiveEffects: List<Effect>
        get() = effects.filter { it.type == "positive" }
    val negativeEffects: List<Effect>
        get() = effects.filter { it.type == "negative" }
    val otherEffects: List<Effect>
        get() = effects.filter { it.type == "neutral" }

    val currentGameTwitch: String
        get() {
            val gameLog = gameLogs.firstOrNull()
            return if(gameLog == null) {
                "-"
            } else {
                val suffix = if(gameLog.game.name.length > 20) ".." else ""
                "${gameLog.game.name.take(20).trim()}$suffix ${gameLog.statusFormatted}"
            }
        }

    val currentGameFullTwitch: String
        get() {
            val gameLog = gameLogs.firstOrNull()
            return if(gameLog == null) {
                "-"
            } else {
                "${gameLog.game.name} ${gameLog.statusFormatted}"
            }
        }

    val currentGameTg: String
        get() {
            val gameLog = gameLogs.firstOrNull()
            if(gameLog == null) {
                return "-"
            } else {
                return """<a href="${gameLog.game.link}"><b>${gameLog.game.name}</b></a> <b>${gameLog.statusFormatted}</b>"""
            }
        }
}

@Serializable
data class ActionLog(
    val text: String,
    val updatedAt: String,
)

@Serializable
data class GameLog(
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val review: String?,
    val game: LogGame
) {
    val statusFormatted: String
        get() = when (status.lowercase()) {
            "completed" -> "✅ Пройдена"
            "rerolled" -> "\uD83D\uDD04 Реролл"
            "dropped" -> "❌ Дроп"
            "freedropped" -> "❎ Дроп без последствий"
            "playing" -> "\uD83D\uDE80 В процессе"
            "selected" -> "\uD83C\uDFAF Выбрана"
            else -> status
        }
}

@Serializable
data class LogGame(
    val name: String,
    val image: String?,
    val link: String,
    val ggp: Int,
)

@Serializable
data class Avatar(
    val image: String
)

@Serializable
data class Level(
    val current: Int,
    val experience: Experience
)

@Serializable
data class Experience(
    val total: Int,
    val current: Int,
    val maximum: Int,
)

@Serializable
data class HitPoints(
    val default: Int,
    val current: Int,
    val maximum: Int,
)

@Serializable
data class PoliceInterest(
    val current: Int,
    val maximum: Int,
)

@Serializable
data class CombatPower(
    val current: Int,
    val maximum: Int,
)

@Serializable
data class Morale(
    val current: Int,
    val maximum: Int,
)

@Serializable
data class PlayerState(
    val main: MainState,
)

@Serializable
data class MainState(
    val value: String,
) {
    val mainStateFormatted: String
        get() = when (value.lowercase()) {
            "rolling game" -> "Поиск игры"
            "map interaction" -> "Взаимодействие с картой"
            "capturing" -> "Захват сектора"
            "capture completion" -> "Завершение захвата"
            else -> value
        }
}

@Serializable
data class Family(
    val members: List<FamilyMember>,
)

@Serializable
data class Inventory(
    val slots: Slots,
)

@Serializable
data class Slots(
    val rings: List<Item>?,
    val pockets: List<Item>?,
    val wheels: List<Item>?,
    val stock: List<Item>?,
    val gloves: Item?,
    val head: Item?,
    val clothes: Item?,
    val body: Item?,
    val belt: Item?,
    val shoes: Item?,
    val chain: Item?,
    val legs: Item?,
) {
    fun getListItem(items: List<Item?>?): String {
        return if (items.isNullOrEmpty()) "Пусто"
        else {
            var res = ""
            for (item in items) {
                if (item == null) continue
                res += item.name + ", "
            }
            if (res.isEmpty()) {
                "Пусто"
            } else {
                res.removeSuffix(", ")
            }
        }
    }

    fun getItem(item: Item?): String {
        return if (item == null) "Пусто"
        else {
            item.name ?: "Пусто"
        }
    }
}

@Serializable
data class Item(
    val name: String?,
)

@Serializable
data class ActionPoints(
    val turns: TurnsActions,
    val movement: MovementActions,
    val exploring: ExploringActions,
)

@Serializable
data class DailyActions(
    val current: Int,
    val maximum: Int,
) {
    override fun toString(): String {
        return "день $current/$maximum"
    }

    fun toTwitchString(): String {
        return "$current/$maximum"
    }
}

@Serializable
data class ExploringActions(
    val current: Int,
    val maximum: Int,
) {
    fun toTwitchString(): String {
        return "ОР:$current/$maximum"
    }

    override fun toString(): String {
        return "Очки разведки $current/$maximum"
    }
}

@Serializable
data class MovementActions(
    val current: Int,
    val maximum: Int,
) {
    override fun toString(): String {
        return "Очки движения $current/$maximum"
    }

    fun toTwitchString(): String {
        return "ОД:$current/$maximum"
    }
}

@Serializable
data class WeeklyActions(
    val current: Int,
    val maximum: Int,
) {
    override fun toString(): String {
        return "Неделя $current/$maximum"
    }

    fun toTwitchString(): String {
        return "Нед.:$current/$maximum"
    }
}

@Serializable
data class TurnsActions(
    val weekly: WeeklyActions,
    val daily: DailyActions,
) {
    override fun toString(): String {
        return "Ходы ${daily}, $weekly"
    }

    fun toTwitchString(): String {
        return "Ходы:${daily.toTwitchString()}, ${weekly.toTwitchString()}"
    }
}

@Serializable
data class Effect(
    val name: String,
    val type: String,
    val description: String,
    val duration: String,
    val source: String
) {
    val typeFormatted: String
        get() = when (type.lowercase()) {
            "positive" -> "Позитивный"
            "negative" -> "Негативный"
            "neutral" -> "Нейтральный"
            else -> type
        }

    override fun toString(): String {
        return "Тип: ${typeFormatted}\nОписание: ${description}\nДлительность: ${duration}\nИсточник: $source"
    }


}

@Serializable
data class Characteristic(
    val persistence: Persistence,
    val fortune: Fortune,
    val diplomacy: Diplomacy,
    val authority: Authority,
    val practicality: Practicality,
    val organization: Organization,
)

@Serializable
data class Persistence(
    val permanent: Int,
    val effects: Int,
    val items: Int,
    val mates: Int,
    val summary: Int,
    val actual: Int,
) {
    val name: String
        get() = "Настойчивость"
}

@Serializable
data class Fortune(
    val permanent: Int,
    val effects: Int,
    val items: Int,
    val mates: Int,
    val summary: Int,
    val actual: Int,
) {
    val name: String
        get() = "Удача"
}

@Serializable
data class Diplomacy(
    val permanent: Int,
    val effects: Int,
    val items: Int,
    val mates: Int,
    val summary: Int,
    val actual: Int,
) {
    val name: String
        get() = "Дипломатия"
}

@Serializable
data class Authority(
    val permanent: Int,
    val effects: Int,
    val items: Int,
    val mates: Int,
    val summary: Int,
    val actual: Int,
) {
    val name: String
        get() = "Авторитет"
}

@Serializable
data class Practicality(
    val permanent: Int,
    val effects: Int,
    val items: Int,
    val mates: Int,
    val summary: Int,
    val actual: Int,
) {
    val name: String
        get() = "Практичность"
}

@Serializable
data class Organization(
    val permanent: Int,
    val effects: Int,
    val items: Int,
    val mates: Int,
    val summary: Int,
    val actual: Int,
) {
    val name: String
        get() = "Организованность"
}

@Serializable
data class Skill(
    val name: String,
    val type: String,
    val cooldown: Int,
    val lore: String,
    val description: String,
) {
    val typeFormatted: String
        get() = when (type.lowercase()) {
            "passive" -> "Пассивный"
            "active" -> "Активный"
            else -> type
        }

    override fun toString(): String {
        return "$lore\nОписание: $description\nТип: $typeFormatted\nКд: $cooldown\n"
    }
}

@Serializable
class FamilyMember(
    val data: FamilyData
)

@Serializable
class FamilyData(
    val name: String,
    val description: String?,
    val skills: List<Skill>? = listOf(),
    val combatPower: Int,
    val tier: Int,
    val image: String?,
) {
    val skillsString: String
        get() {
            return if (skills.isNullOrEmpty()) {
                "Нету"
            } else {
                var result = "\n"
                for (skill in skills) {
                    result += skill.toString() + "\n"
                }
                return result
            }
        }
    val descriptionString: String
        get() {
            return if (description.isNullOrEmpty()) {
                ""
            } else {
                description + "\n"
            }
        }

    override fun toString(): String {
        return "${descriptionString}Тир: $tier\nБоевая мощь: $combatPower\n"
    }
}

data class PlayerExtended(
    val player: Player,
    val onlineOnTwitch: Boolean = false,
    val vkPlayLink: String,
    val currentGameHLTBAvgTime: String,
) {
    val onlineOnTwitchEmoji: String
        get() {
            return if (onlineOnTwitch) {
                "\uD83D\uDCE2"
            } else {
                ""
            }
        }

    val onlineOnTwitchForTelegramEmoji: String
        get() {
            return if (onlineOnTwitch) {
                "\uD83D\uDFE2"
            } else {
                "\uD83D\uDD34"
            }
        }
}