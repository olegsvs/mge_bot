import random
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from webdriver_manager.chrome import ChromeDriverManager
import time
from selenium.webdriver.chrome.options import Options
import pyimgur
import os
from dotenv import load_dotenv
from datetime import datetime
from PIL import Image
import base64


load_dotenv()
IMGUR_ID = os.getenv("IMGUR_ID")
MGE_MAP_URL = os.getenv("MGE_MAP_URL")

# dd/mm/YY H:M:S
# 04.03.2024 05:35 GMT+3
dt_string = datetime.now().strftime("%d.%m.%Y %H:%M") + " GMT+3"

#driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()))
chrome_options = Options()
chrome_options.add_argument("--disable-extensions")
chrome_options.add_argument("--disable-gpu")
chrome_options.add_argument("--no-sandbox")  # linux only
chrome_options.add_argument("--headless")
chrome_options.add_argument("--disable-dev-shm-usage");
chrome_options.add_argument('--remote-debugging-port=9222')
chrome_options.add_argument('--window-size=1920,1080')
#chrome_options.add_argument('--force-device-scale-factor=0.5')
#driver = webdriver.Chrome(executable_path=os.path.abspath(os.getcwd()) + '/chromedriver',options=chrome_options)
driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=chrome_options)
url = MGE_MAP_URL
driver.get(url)
time.sleep(15)
mge_map = 'mge_map.png'
mge_map_header = 'mge_map_header.png'
top_menu = driver.find_element(By.XPATH, '/html/body/div[1]/div[1]')
#player_menu = driver.find_element(By.XPATH, '/html/body/div[1]/div[4]')
#banner = driver.find_element(By.XPATH, '/html/body/div[1]/div[3]')
js_script = '''\
        arguments[0].style.display = 'none';
         '''
driver.execute_script(js_script, top_menu)
#driver.execute_script(js_script, player_menu)
#driver.execute_script(js_script, banner)
#screenshot_area = driver.find_element(By.XPATH, '/html/body/div/div[2]/div[2]/div/div/canvas')

canvas = driver.find_element(By.XPATH, '/html/body/div/div[2]/div[2]/div/div/canvas')
canvas_base64 = driver.execute_script("return arguments[0].toDataURL('image/png').substring(22);", canvas)
canvas_png = base64.b64decode(canvas_base64)
with open(mge_map, 'wb') as f:
    f.write(canvas_png)
map_image = Image.open(mge_map)
width, height = map_image.size
new_size = (width//2, height//2)
resized_image = map_image.resize(new_size)
resized_image.save(mge_map, optimize=False)

map_header_area = driver.find_element(By.XPATH, '/html/body/div/div[2]/div[1]')
with open(mge_map_header, 'wb') as f:
    f.write(map_header_area.screenshot_as_png)
with open('map_update_time.txt', 'w') as f:
    f.write(dt_string)