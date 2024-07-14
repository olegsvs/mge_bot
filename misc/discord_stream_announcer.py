import discord
import asyncio
from discord.ext import tasks
from tinydb import TinyDB
import logging
import threading
import asyncio
import logging
import os
import calendar
import time
from datetime import datetime
import requests
from dotenv import load_dotenv
from tinydb import TinyDB
from typing import Tuple
from typing import Optional

intents = discord.Intents.default()
intents.message_content = True

client = discord.Client(intents=intents)
channel = None
logging.basicConfig(
#    filename=datetime.now().strftime("logs/log_stream_checker_%d_%m_%Y_%H_%M.log"),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    level=logging.INFO,
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)
load_dotenv()
TOKEN = os.getenv("DISCORD_BOT_TOKEN")
TWITCH_CLIENT_ID = os.getenv("TWITCH_CLIENT_ID")
TWITCH_BEARER_TOKEN = os.getenv("TWITCH_BEARER_TOKEN")


@tasks.loop(seconds=60)
async def check_melharucos():
    await check_stream("melharucos")
    
    
@tasks.loop(seconds=60)
async def check_segall():
    await check_stream("segall")


@tasks.loop(seconds=60)
async def check_roadhouse():
    await check_stream("roadhouse")
    
    
@tasks.loop(seconds=60)
async def check_UncleBjorn():
    await check_stream("UncleBjorn")
    
    
@tasks.loop(seconds=60)
async def check_praden():
    await check_stream("praden")        
    

@tasks.loop(seconds=60)
async def check_UselessMouth():
    await check_stream("UselessMouth")      


@tasks.loop(seconds=60)
async def check_guit88man():
    await check_stream("guit88man")      


@tasks.loop(seconds=60)
async def check_Browjey():
    await check_stream("Browjey")      


@tasks.loop(seconds=60)
async def check_f1ashko():
    await check_stream("f1ashko")      

    
async def check_stream(user_login: str):
    verb_form = "завёл"
    db_stream_checker = TinyDB("dbStreamChecker_" + user_login + ".json")
    last_stream_id = -1
    try:
        logger.info("Stream checker: running for " + user_login)
        url = "https://api.twitch.tv/helix/streams?user_login=" + user_login
        my_headers = {
            "Client-ID": TWITCH_CLIENT_ID,
            "Authorization": TWITCH_BEARER_TOKEN,
        }
        response = requests.get(url, headers=my_headers, timeout=15)
        data = response.json()["data"]
        if len(data) != 0 and data[0]["type"] == "live":
            stream = data[0]
            logger.info("Stream checker: streaming " + user_login)
            stream_id = stream["id"]
            if last_stream_id != stream_id:
                last_stream_id = stream_id
                username = stream["user_name"]
                msg = (
                    "👾 "
                    + username
                    + " "
                    + verb_form
                    + " стрим!"
                )
                msg2 = (
                    stream["title"]
                    + "\nКатегория: "
                    + stream["game_name"]
                )
                thumbnail = stream["thumbnail_url"]
                alert_for_stream_id_showed = db_stream_checker.all()
                send_msg = True
                if not alert_for_stream_id_showed:
                    db_stream_checker.insert({"last_stream_id": last_stream_id})
                else:
                    for showed_stream_id in alert_for_stream_id_showed:
                        if str(last_stream_id) in str(showed_stream_id):
                            send_msg = False
                if send_msg:
                    logger.info("Stream checker: Send msg " + user_login)
                    try:
                        embed = discord.Embed(title=msg, description=msg2 + "\n" + "https://www.twitch.tv/" + user_login)
                        thumbnail += '?t=' + str(calendar.timegm(time.gmtime()))
                        thumbnail = thumbnail.replace('{width}', '1920').replace('{height}', '1080')
                        embed.set_image(url=thumbnail)
                        await channel.send(embed=embed)
                        db_stream_checker.insert({"last_stream_id": last_stream_id})
                    except Exception as e:
                        logger.error(
                            "Stream check failed twitch for "
                            + user_login
                            + ",: "
                            + str(e)
                        )
                else:
                    logger.info("Stream checker: Msg already sended " + user_login)
        else:
            logger.info("Stream checker: Not streaming " + user_login)
    except Exception as e:
        logger.error("Stream check failed for " + user_login + ",: " + str(e))

@client.event
async def on_ready():
    global channel 
    channel = client.get_channel(1214448598167719966)
    check_melharucos.start()
    check_Browjey.start()
    check_f1ashko.start()
    check_guit88man.start()
    check_praden.start()
    check_UselessMouth.start()
    check_segall.start()
    check_roadhouse.start()
    check_UncleBjorn.start()


client.run(TOKEN)