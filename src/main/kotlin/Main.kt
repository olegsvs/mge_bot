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
import model.mge.Bases
import model.mge.Player
import model.mge.Players
import model.mge.Trophies
import model.telegraph.*
import model.twitch.CoolDown
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

val logger: Logger = LoggerFactory.getLogger("bot")
val dotenv: Dotenv = Dotenv.load()!!

val botAccessToken = dotenv.get("TWITCH_OAUTH_TOKEN").replace("'", "")
const val minuteInMillis = 60_000L
const val infoRefreshRateTimeMinutes: Int = 10
const val infoRefreshRateTimeMillis: Long = infoRefreshRateTimeMinutes * minuteInMillis // 10m
const val twitchCommandsCoolDownInMillis: Long = 10 * minuteInMillis // 10m
val twitchDefaultRefreshRateTokensTimeMillis = dotenv.get("TWITCH_EXPIRES_IN").replace("'", "").toLong() * 1000

val tgBotToken = dotenv.get("TG_BOT_TOKEN").replace("'", "")
val botOAuth2Credential = OAuth2Credential("twitch", botAccessToken)

val twitchClientId = dotenv.get("TWITCH_CLIENT_ID").replace("'", "")
val twitchClientSecret = dotenv.get("TWITCH_CLIENT_SECRET").replace("'", "")
val telegraphApikey = dotenv.get("TELEGRAPH_API_KEY").replace("'", "")
val tgAdminId = dotenv.get("TG_ADMIN_ID").replace("'", "")
val mgeApiUrl = dotenv.get("MGE_API_JSON_URL").replace("'", "")
val mgeSiteUrl = dotenv.get("MGE_SITE_URL").replace("'", "")
val joinToTwitch = dotenv.get("JOIN_TO_TWITCH_CHANNELS").lowercase() == "true"
val twitchChannels = arrayOf(
    "segall",
    "roadhouse",
    "UncleBjorn",
    "praden",
    "UselessMouth",
    "guit88man",
    "Browjey",
    "f1ashko",
    "melharucos"
)
val vkPlayLinks = mapOf(
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
val telegraphMapper = TelegraphMapper(mgeSiteUrl)
var playersExt: Players = Players(listOf())
var playersExtended: MutableList<PlayerExtended> = mutableListOf()
var trophies: Trophies = Trophies(listOf())
var bases: Bases = Bases(listOf())
var lastDateTimeUpdated = ""
var lastTimeUpdated = ""
var trophiesUrl = ""
var mapUrl = "https://telegra.ph/MGE-Map-07-06"
var editMapUrl = "https://api.telegra.ph/editPage/MGE-Map-07-06"
val coolDowns: MutableList<CoolDown> = mutableListOf()
val tgMessagesToDelete = mutableMapOf<Long, ChatId>()

data class PlayerExtended(
    val player: Player,
    val telegraphUrl: String,
    val inventoryUrl: String,
    val effectsUrl: String,
    val logGamesUrl: String,
    val logActionsUrl: String,
    val onlineOnTwitch: Boolean = false,
    val vkPlayLink: String
) {
    val onlineOnTwitchEmoji: String
        get() {
            return if (onlineOnTwitch) {
                "\uD83D\uDFE2"
            } else {
                "\uD83D\uDD34"
            }
        }
}

val twitchClient: TwitchClient = TwitchClientBuilder.builder()
    .withEnableChat(true)
    .withChatAccount(botOAuth2Credential)
    .withEnableHelix(false)
    .withEnablePubSub(false)
    .withClientId(twitchClientId)
    .withClientSecret(twitchClientSecret)
//    .withFeignLogLevel(feign.Logger.Level.FULL)
    .withDefaultEventHandler(SimpleEventHandler::class.java)
    .build()

val telegraphHttpClient = HttpClient(CIO) {
    expectSuccess = true
    install(Logging) {
        level = LogLevel.ALL
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
//    logLevel = com.github.kotlintelegrambot.logging.LogLevel.All()
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
                            text = "Инфо",
                            url = player.telegraphUrl,
                        ),
                        InlineKeyboardButton.Url(
                            text = "Характеристики",
                            url = "${player.telegraphUrl}#Характеристики",
                        ),
                    ),
                    listOf(
                        InlineKeyboardButton.Url(
                            text = "Семья",
                            url = "${player.telegraphUrl}#Семья",
                        ),
                        InlineKeyboardButton.Url(
                            text = "Инвентарь",
                            url = player.inventoryUrl,
                        ),
                    ),
                    listOf(
                        InlineKeyboardButton.Url(
                            text = "База",
                            url = "${player.telegraphUrl}#База",
                        ),
                        InlineKeyboardButton.Url(
                            text = "Эффекты",
                            url = player.effectsUrl,
                        ),
                    ),
                    listOf(
                        InlineKeyboardButton.Url(
                            text = "Лог действий",
                            url = player.logActionsUrl,
                        ),
                        InlineKeyboardButton.Url(
                            text = "Лог игр",
                            url = player.logGamesUrl,
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
                "tg, ping, message: ${message.text} user: ${message.from?.firstName} ${message.from?.lastName ?: ""} ${message.from?.username ?: ""} ${message.from?.username ?: ""}" +
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
                "tg, start, message: ${message.text} user: ${message.from?.firstName} ${message.from?.lastName ?: ""} ${message.from?.username ?: ""} ${message.from?.username ?: ""}" +
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
                "tg, mge_info, message: ${message.text} user: ${message.from?.firstName} ${message.from?.lastName ?: ""} ${message.from?.username ?: ""} ${message.from?.username ?: ""}" +
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
                refreshMapTask()
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
        for (twitchChannel in twitchChannels) {
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
        if (event.message.equals("!mge_games")) {
            GlobalScope.launch {
                twitchMGEGamesCommand(event, "!mge_games")
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
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm")
        trophies = Gson().fromJson(response, Trophies::class.java)
        playersExt = Gson().fromJson(response, Players::class.java)
        bases = Gson().fromJson(response, Bases::class.java)
        val localLastUpdated = LocalDateTime.now().format(formatter) + " GMT+3"
        val localLastTimeUpdated = LocalDateTime.now().format(timeOnlyFormatter)
        playersExt.players.forEachIndexed { index, player ->
            val telegraphUrl =
                telegraphHttpClient.post("https://api.telegra.ph/editPage/MGE-Player-${index + 1}-07-06") {
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        RootPage(
                            telegraphMapper.mapPlayerToTelegraph(player, index, bases, mapUrl, localLastUpdated),
                            telegraphApikey,
                            "Инфо ${player.name}",
                            returnContent = false
                        )
                    )
                }.body<Root>().result.url
            delay(2000L)
            val inventoryUrl =
                telegraphHttpClient.post("https://api.telegra.ph/editPage/MGE-Player-${index + 1}-inv-07-06") {
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        RootPage(
                            telegraphMapper.mapInventoryToTelegraph(player, localLastUpdated),
                            telegraphApikey,
                            "Инвентарь ${player.name}",
                            returnContent = false
                        )
                    )
                }.body<Root>().result.url
            delay(2000L)
            val effectsUrl =
                telegraphHttpClient.post("https://api.telegra.ph/editPage/MGE-Player-${index + 1}-effects-07-06") {
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        RootPage(
                            telegraphMapper.mapEffectsToTelegraph(player, localLastUpdated),
                            telegraphApikey,
                            "Эффекты ${player.name}",
                            returnContent = false
                        )
                    )
                }.body<Root>().result.url
            delay(2000L)
            val logGamesUrl =
                telegraphHttpClient.post("https://api.telegra.ph/editPage/MGE-Player-${index + 1}-log-games-07-06") {
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        RootPage(
                            telegraphMapper.mapGameLogsToTelegraph(player, localLastUpdated),
                            telegraphApikey,
                            "Лог игр ${player.name}",
                            returnContent = false
                        )
                    )
                }.body<Root>().result.url
            delay(2000L)
            val logActionsUrl =
                telegraphHttpClient.post("https://api.telegra.ph/editPage/MGE-Player-${index + 1}-log-actions-07-06") {
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        RootPage(
                            telegraphMapper.mapActionLogsToTelegraph(player, localLastUpdated),
                            telegraphApikey,
                            "Лог действий ${player.name}",
                            returnContent = false
                        )
                    )
                }.body<Root>().result.url
            delay(2000L)
            var isOnlineOnTwitch = false
            try {
                val twitchStreamResponse =
                    httpClient.get("https://api.twitch.tv/helix/streams?user_login=${player.name}") {
                        header("Client-ID", twitchClientId)
                        header("Authorization", "Bearer $botAccessToken")
                    }.bodyAsText()
                logger.info("twitch, check stream is online, data: $twitchStreamResponse")
                isOnlineOnTwitch = !twitchStreamResponse.contains("\"data\":[]")
            } catch (e: Throwable) {
                logger.error("Failed check stream: ", e)
            }

            playersExtended.add(
                PlayerExtended(
                    player,
                    telegraphUrl,
                    inventoryUrl,
                    effectsUrl,
                    logGamesUrl,
                    logActionsUrl,
                    isOnlineOnTwitch,
                    vkPlayLinks[player.name.lowercase()]!!
                )
            )
        }
        delay(2000L)
        trophiesUrl = telegraphHttpClient.post("https://api.telegra.ph/editPage/MGE-Trofei-07-06") {
            timeout {
                requestTimeoutMillis = 60000
            }
            contentType(ContentType.Application.Json)
            setBody(
                RootPage(
                    telegraphMapper.mapTrophiesToTelegraph(trophies, localLastUpdated),
                    telegraphApikey,
                    "Трофеи",
                    returnContent = false
                )
            )
        }.body<Root>().result.url
        try {
            delay(2000L)
            val mapUpdateTime = File("map_update_time.txt").readText()
            val mapImageUrl = telegraphHttpClient.submitFormWithBinaryData("https://telegra.ph/upload",
                formData = formData {
                    append("description", "mge_map")
                    append("image", File("mge_map.png").readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"mge_map.png\"")
                    })
                }) {
                timeout {
                    requestTimeoutMillis = 60000
                }
            }.body<UploadResponse>().first().src
            delay(2000L)
            val mapHeaderImageUrl = telegraphHttpClient.submitFormWithBinaryData("https://telegra.ph/upload",
                formData = formData {
                    append("description", "mge_map_header")
                    append("image", File("mge_map_header.png").readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"mge_map_header.png\"")
                    })
                }) {
                timeout {
                    requestTimeoutMillis = 60000
                }
            }.body<UploadResponse>().first().src
            delay(2000L)
            telegraphHttpClient.post(editMapUrl) {
                timeout {
                    requestTimeoutMillis = 60000
                }
                contentType(ContentType.Application.Json)
                setBody(
                    RootPage(
                        telegraphMapper.mapMGEMapImageToTelegraph(mapImageUrl, mapHeaderImageUrl, mapUpdateTime),
                        telegraphApikey,
                        "Карта",
                        returnContent = false
                    )
                )
            }.body<Root>()
        } catch (e: Throwable) {
            logger.error("Failed edit map page: ", e)
        }

        try {
            val twitchStreamResponse = httpClient.get("https://api.twitch.tv/helix/streams?user_login=melharucos") {
                header("Client-ID", twitchClientId)
                header("Authorization", "Bearer $botAccessToken")
            }.bodyAsText()
            logger.info("twitch, check stream is online, data: $twitchStreamResponse")
            magistrateIsOnlineOnTwitch = !twitchStreamResponse.contains("\"data\":[]")
        } catch (e: Throwable) {
            logger.error("Failed check stream: ", e)
        }

        lastDateTimeUpdated = localLastUpdated
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
        logger.info("twitch, mge_info, message: ${event.message} user: ${event.user.name}")
        if (!event.permissions.contains(CommandPermission.MODERATOR) && !event.permissions.contains(CommandPermission.BROADCASTER)) {
            logger.info(coolDowns.toString())
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
                        "КД \uD83D\uDD5B ${nextRollMinutes}м${nextRollSeconds}с"
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
                "Upd.$lastTimeUpdated \uD83D\uDD04 раз в ${infoRefreshRateTimeMinutes}m ${getPlayerTwitchInfo(nick)}${
                    getPlayerTphUrl(nick)
                }"
            infoMessage.chunked(499).map {
                event.reply(twitchClient.chat, it)
            }
        } else {
            val shortSummary = playersExt.players.map {
                "${it.name} ${getPlayer(it.name)!!.onlineOnTwitchEmoji} \uD83D\uDC40 Ходы ${it.actionPoints.turns.daily}"
            }
            val infoMessage =
                "Upd.$lastTimeUpdated \uD83D\uDD04 раз в ${infoRefreshRateTimeMinutes}m " + shortSummary.toString()
                    .removeSuffix("]")
                    .removePrefix("[") + " Подробнее !mge_info nick Текущие игры !mge_games"
            infoMessage.chunked(499).map {
                event.reply(twitchClient.chat, it)
            }
        }

    } catch (e: Throwable) {
        logger.error("Failed twitch mge_info command: ", e)
    }
}

fun twitchMGEGamesCommand(event: ChannelMessageEvent, commandText: String) {
    try {
        logger.info("twitch, mge_games, message: ${event.message} user: ${event.user.name}")
        if (!event.permissions.contains(CommandPermission.MODERATOR) && !event.permissions.contains(CommandPermission.BROADCASTER)) {
            logger.info(coolDowns.toString())
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
                        "КД \uD83D\uDD5B ${nextRollMinutes}м${nextRollSeconds}с"
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
        val shortSummary = playersExt.players.map {
            "${it.name} ${getPlayer(it.name)!!.onlineOnTwitchEmoji} ${it.currentGameTwitch}"
        }
        val infoMessage =
            "Upd.$lastTimeUpdated \uD83D\uDD04 раз в ${infoRefreshRateTimeMinutes}m " + shortSummary.toString()
                .removeSuffix("]")
                .removePrefix("[") + " Подробнее !mge_info nick"
        infoMessage.chunked(499).map {
            event.reply(twitchClient.chat, it)
        }
    } catch (e: Throwable) {
        logger.error("Failed twitch mge_info command: ", e)
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun tgMGEInfoCommand(initialMessage: Message) {
    try {
        val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
            playersExt.players.subList(0, 2).map {
                InlineKeyboardButton.CallbackData(
                    text = it.name,
                    callbackData = it.name
                )
            },
            playersExt.players.subList(2, 4).map {
                InlineKeyboardButton.CallbackData(
                    text = it.name,
                    callbackData = it.name
                )
            },
            playersExt.players.subList(4, 6).map {
                InlineKeyboardButton.CallbackData(
                    text = it.name,
                    callbackData = it.name
                )
            },
            playersExt.players.subList(6, 8).map {
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
        val shortSummary = playersExt.players.map {
            ("\uD83D\uDC49 <a href=\"https://www.twitch.tv/${it.name}\"><b>${it.name} ${getPlayer(it.name)!!.onlineOnTwitchEmoji}</b></a> " +
                    " / <a href=\"${getPlayer(it.name)!!.vkPlayLink}\"><b>VK</b></a> \uD83D\uDC40 Ур. <b>" +
                    "${it.level.current}${it.experience}</b> \uD83E\uDEF1 Ходы день <b>${it.actionPoints.turns.daily.current}/" +
                    "${it.actionPoints.turns.daily.maximum}</b>\n\uD83C\uDFAEИгра ${it.currentGameTg}\n").replace(
                " , ", ""
            )
        } + "Судья <a href=\"https://www.twitch.tv/melharucos\"><b>melharucos ${if (magistrateIsOnlineOnTwitch) "\uD83D\uDFE2" else "\uD83D\uDD34"}</b></a>" +
                " / <a href=\"https://live.vkplay.ru/melharucos\"><b>VK</b></a>\n"

        val message = tgBot.sendMessage(
            chatId = ChatId.fromId(initialMessage.chat.id),
            replyMarkup = inlineKeyboardMarkup,
            disableWebPagePreview = true,
            parseMode = ParseMode.HTML,
            text = "⏰Обновлено <b>${lastDateTimeUpdated}</b> \uD83D\uDD04 каждые <b>${infoRefreshRateTimeMinutes}</b> минут\n" + "${
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
    return """👉<a href="https://www.twitch.tv/${playerExt.player.name}"><b>${playerExt.player.name} ${playerExt.onlineOnTwitchEmoji}</b></a> Уровень <b>${playerExt.player.level.current}${playerExt.player.experience}</b>
🎮Текущая игра ${playerExt.player.currentGameTg}
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
        ?: return "Игрок под ником $nick не найден Sadge"
    return """👉 ${playerExt.player.name} ${playerExt.onlineOnTwitchEmoji} Ур.${playerExt.player.level.current}${playerExt.player.experience}
🎮${playerExt.player.currentGameTwitch}
⭐${playerExt.player.actionPoints.turns} ${playerExt.player.actionPoints.movement.toTwitchString()} ${playerExt.player.actionPoints.exploring.toTwitchString()}
Доход ${DecimalFormat("# ##0").format(playerExt.player.dailyIncome)}
На руках ${DecimalFormat("# ##0").format(playerExt.player.money)}
Жетоны съезда ${playerExt.player.congressTokens}
Интерес полиции ${playerExt.player.policeInterest.current}/${playerExt.player.policeInterest.maximum}
Мораль семьи ${playerExt.player.morale.current}/${playerExt.player.morale.maximum}
Эффектов 😊${playerExt.player.positiveEffects.size}😐${playerExt.player.negativeEffects.size}😤${playerExt.player.otherEffects.size}
HP ${playerExt.player.hp.current}/${playerExt.player.hp.maximum}
Боевая мощь ${playerExt.player.combatPower.current}/${playerExt.player.combatPower.maximum}
        """.trimIndent()
}

fun getPlayerTphUrl(nick: String): String {
    val player = playersExtended.firstOrNull { it.player.name.lowercase().trim() == nick.lowercase().trim() }
        ?: return ""
    return " Инфо ${player.telegraphUrl}"
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

fun refreshMapTask() {
    logger.info("refreshMapTask start")
    val processBuilder = ProcessBuilder()
    processBuilder.command("bash", "-c", "cd /home/bot/mge_bot/ && . refresh_map.sh")
    try {
        processBuilder.start()
        logger.info("refreshMapTask process called")
    } catch (e: Throwable) {
        logger.error("Failed call refresh map script:", e)
    }
}

private fun pingCommand(event: ChannelMessageEvent) {
    logger.info("pingCommand")
    try {
        logger.info("twitch, ping, message: ${event.message} user: ${event.user.name}")
        event.reply(
            twitchClient.chat,
            "Starege pong"
        )
    } catch (e: Throwable) {
        logger.error("Failed pingCommand: ", e)
    }
}
