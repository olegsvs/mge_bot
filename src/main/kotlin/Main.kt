import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.events4j.simple.SimpleEventHandler
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import com.google.gson.*
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import model.*
import model.mge.*
import model.twitch.CoolDown
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

val logger: Logger = LoggerFactory.getLogger("bot")
val dotenv: Dotenv = Dotenv.load()!!

val botAccessToken = dotenv.get("TWITCH_OAUTH_TOKEN").replace("'", "")
const val minuteInMillis = 60_000L
const val infoRefreshRateTimeMinutes: Int = 1
const val infoRefreshRateTimeMillis: Long = infoRefreshRateTimeMinutes * minuteInMillis // 1m
const val twitchCommandsCoolDownInMillis: Long = 5 * minuteInMillis // 5m
val twitchDefaultRefreshRateTokensTimeMillis =
    dotenv.get("TWITCH_EXPIRES_IN").replace("'", "").toLong() * 1000 - 5 * minuteInMillis

val tgBotToken = dotenv.get("TG_BOT_TOKEN").replace("'", "")
val botOAuth2Credential = OAuth2Credential("twitch", botAccessToken)

val twitchClientId = dotenv.get("TWITCH_CLIENT_ID").replace("'", "")
val twitchClientSecret = dotenv.get("TWITCH_CLIENT_SECRET").replace("'", "")
val tgAdminId = dotenv.get("TG_ADMIN_ID").replace("'", "")
val mgeApiUrl = dotenv.get("MGE_API_JSON_URL").replace("'", "")
val mgeSiteUrl = dotenv.get("MGE_SITE_URL").replace("'", "")
val mgeDiscordUrl = dotenv.get("MGE_DISCORD_URL").replace("'", "")
val joinToTwitch = dotenv.get("JOIN_TO_TWITCH_CHANNELS").lowercase() == "true"
val twitchAndVKPlayLinks = mapOf(
    "UncleBjorn".lowercase() to "https://live.vkplay.ru/unclebjorn",
    "UselessMouth".lowercase() to "https://live.vkplay.ru/uzya",
    "F1ashko".lowercase() to "https://live.vkplay.ru/f1ashko",
    "BrowJey".lowercase() to "https://vkplay.live/browjey",
    "guit88man".lowercase() to "https://live.vkplay.ru/guitman",
    "RoadHouse".lowercase() to "https://live.vkplay.ru/roadhouse",
    "segall".lowercase() to "https://live.vkplay.ru/segall",
    "praden".lowercase() to "https://live.vkplay.ru/praden",
    "melharucos".lowercase() to "https://live.vkplay.ru/melharucos"
)
var players: List<Player> = listOf()
var playersExtended: MutableList<PlayerExtended> = mutableListOf()
var lastTimeUpdated = ""
val trophiesUrl = "${mgeSiteUrl}/#/trophies"
val mapUrl = "${mgeSiteUrl}/#/map"
val mgeLinksUrl = "${mgeSiteUrl}/#/links"
val mgeBingoUrl = "${mgeSiteUrl}/#/bingo"
val mgeNewsPaperUrl = "${mgeSiteUrl}/#/news"
val mgeRulesUrl = "${mgeSiteUrl}/#/rules"
val coolDowns: MutableList<CoolDown> = mutableListOf()
val tgMessagesToDelete = mutableMapOf<Long, ChatId>()

val twitchClient: TwitchClient = TwitchClientBuilder.builder()
    .withEnableChat(true)
    .withChatAccount(botOAuth2Credential)
    .withEnableHelix(false)
    .withEnablePubSub(false)
    .withClientId(twitchClientId)
    .withClientSecret(twitchClientSecret)
    .withFeignLogLevel(feign.Logger.Level.BASIC)
    .withDefaultEventHandler(SimpleEventHandler::class.java)
    .build()

val httpClient = HttpClient(CIO) {
    expectSuccess = true
    install(Logging) {
        level = LogLevel.INFO
    }
    install(HttpTimeout)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

@OptIn(DelicateCoroutinesApi::class)
val tgBot = bot {
    token = tgBotToken
    dispatch {
        callbackQuery {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            logger.info("tg, callbackQuery, data: ${callbackQuery.data}")
            var nick = callbackQuery.data
            if (nick == "::trophies") {
                bot.sendMessage(ChatId.fromId(chatId), "Трофеи:\n${trophiesUrl}")
                return@callbackQuery
            } else if (nick.contains("::")) nick = nick.split("::")[0]
            val player: PlayerExtended? = getPlayer(nick)
            if (player == null) {
                bot.sendMessage(ChatId.fromId(chatId), "Игрок под ником ${callbackQuery.data} не найден =(")
            } else {
                val markup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.Url(
                            text = "Подробнее",
                            url = mgeSiteUrl,
                        ),
                        InlineKeyboardButton.Url(
                            text = "Дискорд MGE",
                            url = mgeDiscordUrl,
                        ),
                    ),
                    listOf(
                        InlineKeyboardButton.Url(
                            text = "Полезные ссылки",
                            url = mgeLinksUrl,
                        ),
                        InlineKeyboardButton.Url(
                            text = "Бинго",
                            url = mgeBingoUrl,
                        ),
                    ),
                    listOf(
                        InlineKeyboardButton.Url(
                            text = "Газета",
                            url = mgeNewsPaperUrl,
                        ),
                        InlineKeyboardButton.Url(
                            text = "Правила",
                            url = mgeRulesUrl,
                        ),
                    ),
                )
                val message = bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "${getTgUserMention(callbackQuery.from)},\n" + getPlayerTgInfo(callbackQuery.data) + if (isPrivateMessage(
                            callbackQuery.message!!
                        )
                    ) "" else "\n❎Сообщение автоудалится через 5 минут",
                    replyMarkup = markup,
                    parseMode = ParseMode.HTML,
                    disableWebPagePreview = true
                )
                if (!isPrivateMessage(callbackQuery.message!!)) {
                    tgMessagesToDelete[message.get().messageId] = ChatId.fromId(chatId)
                    GlobalScope.launch {
                        delay(5 * 60000L)
                        try {
                            bot.deleteMessage(chatId = ChatId.fromId(chatId), message.get().messageId)
                            tgMessagesToDelete.remove(message.get().messageId)
                        } catch (e: Throwable) {
                            logger.error("Failed delete callback message callbackQuery: ", e)
                        }
                    }
                }
            }
        }
        command("ping") {
            logger.info(
                "tg, ping, message: ${message.text} chat?: ${message.chat.title ?: "None"} user: ${message.from?.firstName} ${message.from?.lastName ?: ""} ${message.from?.username ?: ""} ${message.from?.username ?: ""}" +
                        " ${message.chat.firstName} ${message.chat.lastName ?: ""} ${message.chat.username ?: ""} ${message.chat.username ?: ""}"
            )
            val result = bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Pong!")
            result.fold({
                tgMessagesToDelete[message.messageId] = ChatId.fromId(message.chat.id)
                tgMessagesToDelete[it.messageId] = ChatId.fromId(message.chat.id)
                GlobalScope.launch {
                    delay(60000L)
                    try {
                        bot.deleteMessage(chatId = ChatId.fromId(message.chat.id), it.messageId)
                        tgMessagesToDelete.remove(it.messageId)
                    } catch (e: Throwable) {
                        logger.error("Failed delete ping message callbackQuery: ", e)
                    }
                    try {
                        bot.deleteMessage(chatId = ChatId.fromId(message.chat.id), message.messageId)
                        tgMessagesToDelete.remove(message.messageId)
                    } catch (e: Throwable) {
                        logger.error("Failed delete ping initial message callbackQuery: ", e)
                    }
                }
                logger.info("On ping command")
            }, {
                logger.info("On ping command, error: $it")
            })
        }
        command("start") {
            logger.info(
                "tg, start, message: ${message.text} chat?: ${message.chat.title ?: "None"} user: ${message.from?.firstName} ${message.from?.lastName ?: ""} ${message.from?.username ?: ""} ${message.from?.username ?: ""}" +
                        " ${message.chat.firstName} ${message.chat.lastName ?: ""} ${message.chat.username ?: ""} ${message.chat.username ?: ""}"
            )
            val result = bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "Бот для получение текущей ситуации в мире MGE! /mge_info"
            )
            result.fold({
                logger.info("On start command")
            }, {
                logger.info("On start command, error: $it")
            })
        }
        command("mge_info") {
            logger.info(
                "tg, mge_info, message: ${message.text} chat?: ${message.chat.title ?: "None"} user: ${message.from?.firstName} ${message.from?.lastName ?: ""} ${message.from?.username ?: ""} ${message.from?.username ?: ""}" +
                        " ${message.chat.firstName} ${message.chat.lastName ?: ""} ${message.chat.username ?: ""} ${message.chat.username ?: ""}, chatId:  ${message.chat.id}"
            )
            GlobalScope.launch { tgMGEInfoCommand(message) }
        }
    }
}

private fun getTgUserMention(from: User): String {
    return if (!from.username.isNullOrEmpty()) {
        "@${from.username}"
    } else {
        "${from.firstName} ${if (from.lastName == null) "" else from.lastName}"
    }
}

@Suppress("UNUSED_PARAMETER")
@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    logger.info("Bot started")
    tgBot.startPolling()
    Timer().scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            runBlocking {
                fetchData()
            }
        }
    }, 0L, infoRefreshRateTimeMillis)
    Timer().scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            for (message in tgMessagesToDelete) {
                try {
                    tgBot.deleteMessage(chatId = message.value, message.key)
                } catch (e: Throwable) {
                    logger.error("Failed delete messages before restart: ", e)
                }
            }
            refreshTokensTask()
        }
    }, twitchDefaultRefreshRateTokensTimeMillis, twitchDefaultRefreshRateTokensTimeMillis)

    twitchClient.chat.joinChannel("olegsvs")
    if (joinToTwitch) {
        for (twitchChannel in twitchAndVKPlayLinks.keys) {
            twitchClient.chat.joinChannel(twitchChannel)
        }
    }

    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { event ->
        if (event.message.equals("!mge_info")) {
            GlobalScope.launch {
                twitchMGEInfoCommand(event, "!mge_info")
            }
        }
        if (event.message.startsWith("!mge_info ")) {
            GlobalScope.launch {
                if (event.message.removePrefix("!mge_info ").trim().isEmpty()) {
                    twitchMGEInfoCommand(event, "!mge_info")
                } else {
                    val nick =
                        event.message.removePrefix("!mge_info ").replace("\uDB40\uDC00", "").replace("@", "").trim()
                    twitchMGEInfoCommand(
                        event,
                        commandText = "!mge_info$nick",
                        nick
                    )
                }
            }
        }
        if (event.message.startsWith("!mge_hltb ")) {
            GlobalScope.launch {
                if (event.message.removePrefix("!mge_hltb ").trim().isNotEmpty()) {
                    val gameName =
                        event.message.removePrefix("!mge_hltb ").replace("\uDB40\uDC00", "").replace("@", "").trim()
                    twitchHLTBCommand(
                        event,
                        gameName
                    )
                }
            }
        }
        if (event.message.startsWith("!mping")) {
            pingCommand(event)
        }
    }
}

var magistrateIsOnlineOnTwitch = false
suspend fun fetchData() {
    try {
        val response = httpClient.get(mgeApiUrl).bodyAsText()
        players = Gson().fromJson(response, Players::class.java).players
        val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val localLastTimeUpdated = LocalDateTime.now().format(timeOnlyFormatter)
        players.forEach { player ->
            var isOnlineOnTwitch = false
            try {
                val twitchStreamResponse =
                    httpClient.get("https://api.twitch.tv/helix/streams?user_login=${player.name}") {
                        header("Client-ID", twitchClientId)
                        header("Authorization", "Bearer $botAccessToken")
                    }.bodyAsText()
                isOnlineOnTwitch = twitchStreamResponse.contains("viewer_count")
                logger.info("twitch, check stream is online, player: ${player.name}, result: ${isOnlineOnTwitch}, data: $twitchStreamResponse")
            } catch (e: Throwable) {
                logger.error("Failed check stream: ", e)
            }
            var currentGameHLTBAvgTime = ""
            try {
                player.gameLogs.firstOrNull()?.let {
                    val hltbProxyResponse =
                        httpClient.get(
                            "https://hltb-proxy.fly.dev/v1/query?title=${
                                URLEncoder.encode(
                                    it.game.name.replace(
                                        "™",
                                        ""
                                    ).replace(":",""), "utf-8"
                                )
                            }"
                        ).bodyAsText()
                    val part = hltbProxyResponse.subSequence(
                        hltbProxyResponse.indexOf("avgSeconds") + "avgSeconds".length + 2,
                        hltbProxyResponse.length
                    )
                    val seconds = part.subSequence(0, part.indexOf(",")).toString().toInt()
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    if (hours != 0 || minutes != 0) {
                        currentGameHLTBAvgTime = "HLTB:${hours}ч${minutes}м"
                    }
                    logger.info("hltb: ${it.game.name}, result: ${currentGameHLTBAvgTime}, data: $hltbProxyResponse")
                }
            } catch (e: Throwable) {
                logger.error("Failed get hltb: ", e)
            }
            val playerExt =
                playersExtended.firstOrNull { it.player.name.lowercase().trim() == player.name.lowercase().trim() }
            if (playerExt != null) {
                playersExtended[playersExtended.indexOf(playerExt)] = PlayerExtended(
                    player,
                    isOnlineOnTwitch,
                    twitchAndVKPlayLinks[player.name.lowercase()]!!,
                    currentGameHLTBAvgTime
                )
            } else {
                playersExtended.add(
                    PlayerExtended(
                        player,
                        isOnlineOnTwitch,
                        twitchAndVKPlayLinks[player.name.lowercase()]!!,
                        currentGameHLTBAvgTime
                    )
                )
            }
        }
        try {
            val twitchStreamResponse = httpClient.get("https://api.twitch.tv/helix/streams?user_login=melharucos") {
                header("Client-ID", twitchClientId)
                header("Authorization", "Bearer $botAccessToken")
            }.bodyAsText()
            logger.info("twitch, check stream is online, data: $twitchStreamResponse")
            magistrateIsOnlineOnTwitch = twitchStreamResponse.contains("viewer_count")
        } catch (e: Throwable) {
            logger.error("Failed check stream: ", e)
        }
        lastTimeUpdated = localLastTimeUpdated
    } catch (e: Throwable) {
        try {
            tgBot.sendMessage(
                chatId = ChatId.fromId(tgAdminId.toLong()),
                text = e.toString()
            )
        } catch (e: Throwable) {
            logger.error("Failed fetchData: ", e)
        }
        logger.error("Failed fetchData: ", e)
    }

}

fun twitchMGEInfoCommand(event: ChannelMessageEvent, commandText: String, nick: String? = null) {
    try {
        logger.info("twitch, mge_info, message: ${event.message} channel: ${event.channel.name} user: ${event.user.name}")
        if (lastTimeUpdated.isEmpty()) {
            event.reply(twitchClient.chat, "Обновляемся, попробуйте через минуту...")
            return
        }
        if (!event.permissions.contains(CommandPermission.MODERATOR) && !event.permissions.contains(CommandPermission.BROADCASTER)) {
            val cd = coolDowns.firstOrNull { it.channelName == event.channel!!.name && it.commandText == commandText }
            if (cd != null) {
                val now = System.currentTimeMillis() / 1000
                val cdInSeconds = (cd.coolDownMillis / 1000)
                val diff = (now - cd.lastUsageInMillis / 1000)
                if (diff < cdInSeconds) {
                    val nextRollTime = (cdInSeconds - diff)
                    val nextRollMinutes = (nextRollTime % 3600) / 60
                    val nextRollSeconds = (nextRollTime % 3600) % 60
                    event.reply(
                        twitchClient.chat,
                        "Для команды ${commandText.replace("!mge_info", "!mge_info ").trim()} КД \uD83D\uDD5B ${nextRollMinutes}м${nextRollSeconds}с"
                    )
                    return
                } else {
                    coolDowns.remove(cd)
                }
            }
            coolDowns.add(
                CoolDown(
                    channelName = event.channel!!.name,
                    commandText = commandText,
                    coolDownMillis = twitchCommandsCoolDownInMillis,
                    lastUsageInMillis = System.currentTimeMillis()
                )
            )
        }
        if (!nick.isNullOrEmpty()) {
            val infoMessage =
                "${getPlayerTwitchInfo(nick)} Подробнее: $mgeSiteUrl"
            infoMessage.chunked(499).map {
                event.reply(twitchClient.chat, it)
            }
        } else {
            val shortSummary = players.map {
                "${it.name} ${getPlayer(it.name)!!.onlineOnTwitchEmoji} [${it.currentGameTwitch}] " +
                        "ДХ:${it.actionPoints.turns.daily.toTwitchString()}"
            }
            val infoMessage = shortSummary.toString()
                .removeSuffix("]")
                .removePrefix("[") + " Подробнее !mge_info ник, !mge_hltb игра"
            infoMessage.chunked(499).map {
                event.reply(twitchClient.chat, it)
            }
        }

    } catch (e: Throwable) {
        logger.error("Failed twitch mge_info command: ", e)
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun twitchHLTBCommand(event: ChannelMessageEvent, gameName: String) {
    try {
        logger.info("twitch, hltb, gameName: ${gameName}, message: ${event.message} channel: ${event.channel.name} user: ${event.user.name}")
        val hltbProxyResponse =
            httpClient.get(
                "https://hltb-proxy.fly.dev/v1/query?title=${
                    URLEncoder.encode(
                        gameName.replace("™", "").replace(":",""),
                        "utf-8"
                    )
                }"
            ).bodyAsText()
        if (hltbProxyResponse.length < 10) {
            event.reply(twitchClient.chat, "Не найдено Sadge")
            return
        }
        val partName = hltbProxyResponse.subSequence(
            hltbProxyResponse.indexOf("gameName") + "gameName".length + 2,
            hltbProxyResponse.length
        )
        val gameNameResult =
            partName.subSequence(0, partName.indexOf(",\"")).toString().removePrefix("\"").removeSuffix("\"")
        val part = hltbProxyResponse.subSequence(
            hltbProxyResponse.indexOf("avgSeconds") + "avgSeconds".length + 2,
            hltbProxyResponse.length
        )
        val seconds = part.subSequence(0, part.indexOf(",")).toString().toInt()
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        if (hours != 0 || minutes != 0) {
            val result = "$gameNameResult HLTB AVG:${hours}ч${minutes}м"
            event.reply(twitchClient.chat, result)
            logger.info("hltb: ${gameName}, result: ${result}, data: $hltbProxyResponse")
        } else {
            val result = "$gameNameResult записей о времени не найдено Sadge"
            event.reply(twitchClient.chat, result)
            logger.info("hltb: ${gameName}, data: $hltbProxyResponse")
        }
    } catch (e: Throwable) {
        logger.error("Failed twitch hltb command: ", e)
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun tgMGEInfoCommand(initialMessage: Message) {
    try {
        if (lastTimeUpdated.isEmpty()) {
            tgBot.sendMessage(
                chatId = ChatId.fromId(initialMessage.chat.id),
                disableWebPagePreview = true,
                parseMode = ParseMode.HTML,
                text = "Обновляемся, попробуйте через минуту..."
            )
            return
        }
        val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
            players.subList(0, 2).map {
                InlineKeyboardButton.CallbackData(
                    text = it.name,
                    callbackData = it.name
                )
            },
            players.subList(2, 4).map {
                InlineKeyboardButton.CallbackData(
                    text = it.name,
                    callbackData = it.name
                )
            },
            players.subList(4, 6).map {
                InlineKeyboardButton.CallbackData(
                    text = it.name,
                    callbackData = it.name
                )
            },
            players.subList(6, 8).map {
                InlineKeyboardButton.CallbackData(
                    text = it.name,
                    callbackData = it.name
                )
            },
            listOf(
                InlineKeyboardButton.Url(
                    text = "Трофеи",
                    url = trophiesUrl,
                ),
                InlineKeyboardButton.Url(
                    text = "Карта",
                    url = mapUrl,
                ),
                InlineKeyboardButton.Url(
                    text = "Сайт MGE",
                    url = mgeSiteUrl,
                ),
            ),
        )

        val shortSummary = players.map {
            var twitchGameFormatted = ""
            if (getPlayer(it.name)!!.currentGameHLTBAvgTime.isNotEmpty()) {
                twitchGameFormatted = "\n\uD83D\uDD54" + getPlayer(it.name)!!.currentGameHLTBAvgTime
            }else {
                twitchGameFormatted = "\n\uD83D\uDD54 HLTB: -"
            }
            ("\uD83D\uDC49 <a href=\"https://www.twitch.tv/${it.name}\"><b>${it.name} ${getPlayer(it.name)!!.onlineOnTwitchForTelegramEmoji}</b></a>" +
                    " / <a href=\"${getPlayer(it.name)!!.vkPlayLink}\"><b>VK</b></a> \uD83D\uDC40" +
                    " Ходы <b>${it.actionPoints.turns.daily.current}/" +
                    "${it.actionPoints.turns.daily.maximum}</b> Нед <b>${it.actionPoints.turns.weekly.current}/${it.actionPoints.turns.weekly.maximum}</b>" +
                    "\n\uD83C\uDFAEИгра ${it.currentGameTg}${twitchGameFormatted}\n\n").replace(
                " , ", ""
            )
        } + "Судья <a href=\"https://www.twitch.tv/melharucos\"><b>melharucos ${if (magistrateIsOnlineOnTwitch) "\uD83D\uDFE2" else "\uD83D\uDD34"}</b></a>" +
                " / <a href=\"https://live.vkplay.ru/melharucos\"><b>VK</b></a>\n"

        val message = tgBot.sendMessage(
            chatId = ChatId.fromId(initialMessage.chat.id),
            replyMarkup = inlineKeyboardMarkup,
            disableWebPagePreview = true,
            parseMode = ParseMode.HTML,
            text = "${
                shortSummary.toString().removeSuffix("]").removePrefix("[").replace(", ", "")
            }${if (isPrivateMessage(initialMessage)) "" else "❎Сообщение автоудалится через <b>5</b> минут\n"}✅Выберите стримера для получения сводки\uD83D\uDC47"
        )
        if (!isPrivateMessage(initialMessage)) {
            tgMessagesToDelete[message.get().messageId] = ChatId.fromId(initialMessage.chat.id)
            tgMessagesToDelete[initialMessage.messageId] = ChatId.fromId(initialMessage.chat.id)
            GlobalScope.launch {
                delay(5 * 60000L)
                try {
                    tgBot.deleteMessage(chatId = ChatId.fromId(initialMessage.chat.id), message.get().messageId)
                    tgMessagesToDelete.remove(message.get().messageId)
                } catch (e: Throwable) {
                    logger.error("Failed delete message tgMGEInfoCommand", e)
                }
                try {
                    tgBot.deleteMessage(chatId = ChatId.fromId(initialMessage.chat.id), initialMessage.messageId)
                    tgMessagesToDelete.remove(initialMessage.messageId)
                } catch (e: Throwable) {
                    logger.error("Failed delete initialMessage tgMGEInfoCommand", e)
                }
            }
        }
    } catch (e: Throwable) {
        logger.error("Failed tgMGEInfoCommand: ", e)
    }
}

private fun isPrivateMessage(message: Message): Boolean {
    return !message.chat.id.toString().startsWith("-100")
}

fun getPlayerTgInfo(nick: String): String {
    val playerExt = playersExtended.firstOrNull { it.player.name.lowercase().trim() == nick.lowercase().trim() }
        ?: return "Игрок под ником <b>$nick</b> не найден Sadge"
    return """👉<a href="https://www.twitch.tv/${playerExt.player.name}"><b>${playerExt.player.name} ${playerExt.onlineOnTwitchForTelegramEmoji}</b></a> Уровень <b>${playerExt.player.level.current}${playerExt.player.experience}</b>
🎮Текущая игра ${playerExt.player.currentGameTg}
🕔${playerExt.currentGameHLTBAvgTime.ifEmpty { "HLTB: -" }}
🤔Состояние ${playerExt.player.states.main.mainStateFormatted}
⭐Ходы день <b>${playerExt.player.actionPoints.turns.daily.current}/${playerExt.player.actionPoints.turns.daily.maximum}</b>, неделя <b>${playerExt.player.actionPoints.turns.weekly.current}/${playerExt.player.actionPoints.turns.weekly.maximum}</b>
⭐Очки движения <b>${playerExt.player.actionPoints.movement.current}/${playerExt.player.actionPoints.movement.maximum}</b>
⭐Очки разведки <b>${playerExt.player.actionPoints.exploring.current}/${playerExt.player.actionPoints.exploring.maximum}</b>
💰Доход в день <b>${DecimalFormat("# ##0.00").format(playerExt.player.dailyIncome)}</b> На руках💰<b>${
        DecimalFormat("# ##0.00").format(
            playerExt.player.money
        )
    }</b>
🗣Жетоны съезда <b>${playerExt.player.congressTokens}</b>
👮Интерес полиции <b>${playerExt.player.policeInterest.current}/${playerExt.player.policeInterest.maximum}</b>
🔱Мораль семьи <b>${playerExt.player.morale.current}/${playerExt.player.morale.maximum}</b>
❔Эффектов 😊<b>${playerExt.player.positiveEffects.size}</b>😐<b>${playerExt.player.negativeEffects.size}</b>😤<b>${playerExt.player.otherEffects.size}</b>
❤HP <b>${playerExt.player.hp.current}/${playerExt.player.hp.maximum}</b>
💪Боевая мощь <b>${playerExt.player.combatPower.current}/${playerExt.player.combatPower.maximum}</b>
        """.trimIndent()
}

fun getPlayerTwitchInfo(nick: String): String {
    val playerExt = playersExtended.firstOrNull { it.player.name.lowercase().trim() == nick.lowercase().trim() }
        ?: return "Игрок не найден Sadge"
    return """${playerExt.player.name} ${playerExt.onlineOnTwitchEmoji} УР${playerExt.player.level.current},
🎮${playerExt.player.currentGameTwitch}${if (playerExt.currentGameHLTBAvgTime.isEmpty()) "," else ", " + playerExt.currentGameHLTBAvgTime + ","}
⭐${playerExt.player.actionPoints.turns.toTwitchString()}, ${playerExt.player.actionPoints.movement.toTwitchString()}, ${playerExt.player.actionPoints.exploring.toTwitchString()},
Статус:${playerExt.player.states.main.mainStateFormatted},
ДД:${DecimalFormat("# ##0").format(playerExt.player.dailyIncome).trim()},
Битсов:${DecimalFormat("# ##0").format(playerExt.player.money).trim()},
ЖС:${playerExt.player.congressTokens},
Копы:${playerExt.player.policeInterest.current}/${playerExt.player.policeInterest.maximum},
МС:${playerExt.player.morale.current}/${playerExt.player.morale.maximum},
Эффекты:😊${playerExt.player.positiveEffects.size}😐${playerExt.player.negativeEffects.size}😤${playerExt.player.otherEffects.size},
HP:${playerExt.player.hp.current}/${playerExt.player.hp.maximum},
БМ:${playerExt.player.combatPower.current}/${playerExt.player.combatPower.maximum},
        """.trimIndent()
}

fun getPlayer(nick: String): PlayerExtended? {
    return playersExtended.firstOrNull { it.player.name.lowercase().trim() == nick.lowercase().trim() }
}

fun refreshTokensTask() {
    logger.info("refreshTokensTask start")
    val processBuilder = ProcessBuilder()
    processBuilder.command("bash", "-c", "cd /home/bot/mge_bot/ && . jrestart.sh")
    try {
        processBuilder.start()
        logger.info("refreshTokensTask process called")
    } catch (e: Throwable) {
        logger.error("Failed call restart script:", e)
    }
}

private fun pingCommand(event: ChannelMessageEvent) {
    logger.info("pingCommand")
    try {
        logger.info("twitch, ping, message: ${event.message} channel: ${event.channel.name} user: ${event.user.name}")
        event.reply(
            twitchClient.chat,
            "Starege pong"
        )
    } catch (e: Throwable) {
        logger.error("Failed pingCommand: ", e)
    }
}