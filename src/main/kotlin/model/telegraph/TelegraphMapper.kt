package model.telegraph

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import model.mge.Bases
import model.mge.Player
import model.mge.Trophies
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TelegraphMapper(private val mgeSiteUrl: String) {
    //2024-02-20T18:47:39.348Z
    private val apiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private val botFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun mapEffectsToTelegraph(player: Player, localLastUpdated: String): List<Content> {
        val content: MutableList<Content> = mutableListOf()
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Обновлено: $localLastUpdated"))
            )
        )
        //Effects
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = "#Отрицательные"),
                children = Json.encodeToJsonElement(listOf("Отрицательные")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = "#Другие"),
                children = Json.encodeToJsonElement(listOf("Другие")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "h4",
                attrs = Attrs("Положительные:"),
                children = Json.encodeToJsonElement(
                    listOf(
                        Content(
                            tag = "a",
                            attrs = Attrs(href = "#Положительные"),
                            children = Json.encodeToJsonElement(listOf("Положительные")),
                        )
                    )
                ),
            )
        )
        for (effect in player.positiveEffects) {
            content.add(
                Content(
                    tag = "b",
                    children = Json.encodeToJsonElement(listOf(effect.name + "\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf(effect.toString()))
                )
            )
        }
        content.add(
            Content(
                tag = "h4",
                attrs = Attrs("Отрицательные"),
                children = Json.encodeToJsonElement(
                    listOf(
                        Content(
                            tag = "a",
                            attrs = Attrs(href = "#Отрицательные"),
                            children = Json.encodeToJsonElement(listOf("Отрицательные")),
                        )
                    )
                ),
            )
        )
        for (effect in player.negativeEffects) {
            content.add(
                Content(
                    tag = "b",
                    children = Json.encodeToJsonElement(listOf(effect.name + "\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf(effect.toString()))
                )
            )
        }
        content.add(
            Content(
                tag = "h4",
                attrs = Attrs("Другие"),
                children = Json.encodeToJsonElement(
                    listOf(
                        Content(
                            tag = "a",
                            attrs = Attrs(href = "#Другие"),
                            children = Json.encodeToJsonElement(listOf("Другие")),
                        )
                    )
                ),
            )
        )
        for (effect in player.otherEffects) {
            content.add(
                Content(
                    tag = "b",
                    children = Json.encodeToJsonElement(listOf(effect.name + "\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf(effect.toString()))
                )
            )
        }
        return content
    }

    fun mapInventoryToTelegraph(player: Player, localLastUpdated: String): List<Content> {
        val content: MutableList<Content> = mutableListOf()
        //Inventory
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Обновлено: $localLastUpdated"))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Кольца: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getListItem(player.inventory.slots.rings)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Карманы: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getListItem(player.inventory.slots.pockets)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Колесо приколов: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getListItem(player.inventory.slots.wheels)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Инвентарь: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getListItem(player.inventory.slots.stock)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Перчатки: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.gloves)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Голова: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.head)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Туловище: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.body)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Одежда: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.clothes)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Ремень: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.belt)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Обувь: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.shoes)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Цепочка: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.chain)))
            )
        )
        content.add(
            Content(
                tag = "b",
                children = Json.encodeToJsonElement(listOf("Ноги: \n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf(player.inventory.slots.getItem(player.inventory.slots.legs)))
            )
        )
        return content
    }

    fun mapTrophiesToTelegraph(trophies: Trophies, localLastUpdated: String): List<Content> {
        val content: MutableList<Content> = mutableListOf()
        //Trophies
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Обновлено: $localLastUpdated"))
            )
        )
        for (trophy in trophies.trophies) {
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf(trophy.name + "\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf(trophy.toString()))
                )
            )
            trophy.leaderboard.list.sortedByDescending { it.trophyScore.toIntOrNull() }
                .forEachIndexed { index, leader ->
                    content.add(
                        Content(
                            tag = "p",
                            children = Json.encodeToJsonElement(listOf("${index + 1}. $leader"))
                        )
                    )
                }
        }
        return content
    }

    fun mapPlayerToTelegraph(player: Player, index: Int, bases: Bases, mapUrl: String, localLastUpdated: String): List<Content> {
        val content: MutableList<Content> = mutableListOf()
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Обновлено: $localLastUpdated"))
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = mgeSiteUrl),
                children = Json.encodeToJsonElement(listOf("Сайт MGE")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = "https://telegra.ph/MGE-Trofei-07-06"),
                children = Json.encodeToJsonElement(listOf("Трофеи")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = mapUrl),
                children = Json.encodeToJsonElement(listOf("Карта")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = "https://telegra.ph/MGE-Player-${index + 1}-inv-07-06"),
                children = Json.encodeToJsonElement(listOf("Инвентарь ${player.name}")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = "https://telegra.ph/MGE-Player-${index + 1}-effects-07-06"),
                children = Json.encodeToJsonElement(listOf("Эффекты ${player.name}")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = "https://telegra.ph/MGE-Player-${index + 1}-log-actions-07-06"),
                children = Json.encodeToJsonElement(listOf("Лог действий ${player.name}")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        content.add(
            Content(
                tag = "a",
                attrs = Attrs(href = "https://telegra.ph/MGE-Player-${index + 1}-log-games-07-06"),
                children = Json.encodeToJsonElement(listOf("Лог игр ${player.name}")),
            )
        )
        content.add(
            Content(
                tag = "br",
            )
        )
        //Characteristics
        content.add(
            Content(
                tag = "h4",
                attrs = Attrs("Характеристики"),
                children = Json.encodeToJsonElement(
                    listOf(
                        Content(
                            tag = "a",
                            attrs = Attrs(href = "#Характеристики"),
                            children = Json.encodeToJsonElement(listOf("Характеристики")),
                        )
                    )
                ),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.characteristics.authority.name}: ${player.characteristics.authority.actual}\n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.characteristics.diplomacy.name}: ${player.characteristics.diplomacy.actual}\n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.characteristics.persistence.name}: ${player.characteristics.persistence.actual}\n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.characteristics.fortune.name}: ${player.characteristics.fortune.actual}\n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.characteristics.practicality.name}: ${player.characteristics.practicality.actual}\n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.characteristics.organization.name}: ${player.characteristics.organization.actual}\n")),
            )
        )
        //Actions Points
        content.add(
            Content(
                tag = "h4",
                attrs = Attrs("Очки действий"),
                children = Json.encodeToJsonElement(
                    listOf(
                        Content(
                            tag = "a",
                            attrs = Attrs(href = "#Очки_действий"),
                            children = Json.encodeToJsonElement(listOf("Очки действий")),
                        )
                    )
                ),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.actionPoints.turns}\n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.actionPoints.movement}\n")),
            )
        )
        content.add(
            Content(
                tag = "blockquote",
                children = Json.encodeToJsonElement(listOf("${player.actionPoints.exploring}\n")),
            )
        )
        //Base
        content.add(
            Content(
                tag = "h4",
                attrs = Attrs("База"),
                children = Json.encodeToJsonElement(
                    listOf(
                        Content(
                            tag = "a",
                            attrs = Attrs(href = "#База"),
                            children = Json.encodeToJsonElement(listOf("База")),
                        )
                    )
                ),
            )
        )
        val base = bases.map.filter { it.sector.type == "BASE" }
            .firstOrNull { it.sector.data.dynamicData?.controlledBy.equals(player.id) }?.sector?.data?.dynamicData?.structures
        if(base != null) {
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("${base.arsenal.name}, уровень: ${base.arsenal.level}\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("${base.familyClub.name}, уровень: ${base.familyClub.level}\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("${base.garage.name}, уровень: ${base.garage.level}\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("${base.stock.name}, уровень: ${base.stock.level}\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("${base.pub.name}, уровень: ${base.pub.level}\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("${base.headquarter.name}, уровень: ${base.headquarter.level}\n")),
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("${base.gamblingClub.name}, уровень: ${base.gamblingClub.level}\n")),
                )
            )
        }
        //Family
        content.add(
            Content(
                tag = "h4",
                attrs = Attrs("Семья"),
                children = Json.encodeToJsonElement(
                    listOf(
                        Content(
                            tag = "a",
                            attrs = Attrs(href = "#Семья"),
                            children = Json.encodeToJsonElement(listOf("Семья")),
                        )
                    )
                ),
            )
        )
        for (member in player.family.members) {
            content.add(
                Content(
                    tag = "p",
                    children = Json.encodeToJsonElement(
                        listOf(
                            Content(
                                tag = "strong",
                                children = Json.encodeToJsonElement(listOf(member.data.name + "\n")),
                            )
                        )
                    ),
                )
            )
            if (member.data.image != null)
                content.add(
                    Content(
                        tag = "p",
                        children = Json.encodeToJsonElement(
                            listOf(
                                Content(
                                    tag = "img",
                                    attrs = Attrs(
                                        src = "${mgeSiteUrl}/assets/${
                                            member.data.image.replace(
                                                " ",
                                                "%20"
                                            )
                                        }"
                                    ),
                                )
                            )
                        )
                    )
                )
            if(member.data.skills.isNullOrEmpty()) {
                content.add(
                    Content(
                        tag = "blockquote",
                        children = Json.encodeToJsonElement(listOf(member.data.toString() + "Скиллы: -"))
                    )
                )
            } else {
                content.add(
                    Content(
                        tag = "blockquote",
                        children = Json.encodeToJsonElement(listOf(member.data.toString()))
                    )
                )
                for(skill in member.data.skills) {
                    content.add(
                        Content(
                            tag = "blockquote",
                            children = Json.encodeToJsonElement(listOf(Content(
                                tag = "a",
                                children = Json.encodeToJsonElement(listOf(
                                    Content(
                                        tag = "a",
                                        children = Json.encodeToJsonElement(listOf("Скиллы:\n"))
                                    ),
                                    Content(
                                        tag = "strong",
                                        children = Json.encodeToJsonElement(listOf(skill.name + "\n"))
                                    ),
                                    Content(
                                        tag = "a",
                                        children = Json.encodeToJsonElement(listOf(skill.toString()))
                                    )))
                            )))
                        )
                    )
                    /*content.add(
                        Content(
                            tag = "blockquote",
                            children = Json.encodeToJsonElement(listOf(skill.toString()))
                        )
                    )*/
                }
            }

        }
        return content
    }

    fun mapGameLogsToTelegraph(player: Player, localLastUpdated: String): List<Content> {
        val content: MutableList<Content> = mutableListOf()
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Обновлено: $localLastUpdated"))
            )
        )
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Последние 30 записей"))
            )
        )
        //GameLogs
        for (gameLog in player.gameLogs.take(30)) {
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(
                        listOf(
                            Content(
                                tag = "a",
                                attrs = Attrs(href = gameLog.game.link),
                                children = Json.encodeToJsonElement(
                                    listOf(
                                        Content(
                                            tag = "strong",
                                            children = Json.encodeToJsonElement(listOf(gameLog.game.name))
                                        )
                                    )
                                ),
                            )
                        )
                    )
                )
            )
            if (!gameLog.game.image.isNullOrEmpty()
                && !gameLog.game.image.contains("/img/gog/")
                && gameLog.game.image.startsWith("http")
            ) {
                content.add(
                    Content(
                        tag = "img",
                        attrs = Attrs(
                            src = gameLog.game.image
                        ),
                    )
                )
            }
            var dateTimeText = ""
            try {
                dateTimeText = "Запись создана: ${
                    LocalDateTime.parse(gameLog.createdAt, apiFormat).format(botFormat)
                }, обновлена: ${LocalDateTime.parse(gameLog.updatedAt, apiFormat).format(botFormat)}"
            } catch (_: Throwable) {
            }
            if (dateTimeText.isNotEmpty())
                content.add(
                    Content(
                        tag = "blockquote",
                        children = Json.encodeToJsonElement(listOf(dateTimeText))
                    )
                )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("GGP: ${gameLog.game.ggp}"))
                )
            )
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(listOf("Статус: ${gameLog.statusFormatted}"))
                )
            )
            if (!gameLog.review.isNullOrEmpty())
                content.add(
                    Content(
                        tag = "blockquote",
                        children = Json.encodeToJsonElement(listOf("Отзыв: ${gameLog.review}"))
                    )
                )
            content.add(
                Content(
                    tag = "p",
                    children = Json.encodeToJsonElement(listOf("\n"))
                )
            )
        }
        return content
    }

    fun mapActionLogsToTelegraph(player: Player, localLastUpdated: String): List<Content> {
        val content: MutableList<Content> = mutableListOf()
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Обновлено: $localLastUpdated"))
            )
        )
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Последние 50 записей"))
            )
        )
        //ActionLogs
        for (actionLog in player.logs.take(50)) {
            var dateTimeText = actionLog.text
            try {
                dateTimeText =
                    "${LocalDateTime.parse(actionLog.updatedAt, apiFormat).format(botFormat)} ${actionLog.text}"
            } catch (_: Throwable) {
            }
            content.add(
                Content(
                    tag = "blockquote",
                    children = Json.encodeToJsonElement(
                        listOf(dateTimeText)
                    )
                )
            )
        }
        return content
    }

    fun mapMGEMapImageToTelegraph(mapUrl: String, mapHeaderUrl: String, localLastUpdated: String): List<Content> {
        val content: MutableList<Content> = mutableListOf()
        content.add(
            Content(
                tag = "pre",
                children = Json.encodeToJsonElement(listOf("Обновлено: $localLastUpdated"))
            )
        )
        //MGEMap
        content.add(
            Content(
                tag = "img",
                attrs = Attrs(
                    src = mapHeaderUrl
                ),
            )
        )
        content.add(
            Content(
                tag = "img",
                attrs = Attrs(
                    src = mapUrl
                ),
            )
        )
        return content
    }
}
