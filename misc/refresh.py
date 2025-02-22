import requests
from dotenv import load_dotenv
import dotenv
import os

load_dotenv()

tg_token = os.getenv("TG_BOT_TOKEN")
tg_admin_id = os.getenv("TG_ADMIN_ID")
def send_msg(text):
   url_req = "https://api.telegram.org/bot" + tg_token + "/sendMessage" + "?chat_id=" + tg_admin_id + "&text=" + text
   results = requests.get(url_req)


try:
    token_bot = os.getenv("TWITCH_OAUTH_TOKEN")
    refresh_token_bot = os.getenv("TWITCH_REFRESH_TOKEN")
    #refresh_token_test_channel =  os.getenv("CR")
    url = "https://id.twitch.tv/oauth2/token"

    payload='grant_type=refresh_token&refresh_token=' + refresh_token_bot + '&client_id=&client_secret='
    headers = {
      'Content-Type': 'application/x-www-form-urlencoded'
    }

    response = requests.request("POST", url, headers=headers, data=payload)

    print(response.text)
    ACCESS_BOT_TOKEN=response.json()['access_token']
    REFRESH_BOT_TOKEN=response.json()['refresh_token']
    TIME_BOT_TOKEN=response.json()['expires_in']
    dotenv.set_key('.env', "TWITCH_OAUTH_TOKEN", ACCESS_BOT_TOKEN)
    dotenv.set_key('.env', "TWITCH_REFRESH_TOKEN", REFRESH_BOT_TOKEN)
    dotenv.set_key('.env', "TWITCH_EXPIRES_IN", str(TIME_BOT_TOKEN))
except Exception as e:
    send_msg(str(e))