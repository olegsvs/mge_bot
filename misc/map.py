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
chrome_options.add_argument('--window-size=2000,2000')
#chrome_options.add_argument('--force-device-scale-factor=0.1')
#driver = webdriver.Chrome(executable_path=os.path.abspath(os.getcwd()) + '/chromedriver',options=chrome_options)
driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=chrome_options)
url = MGE_MAP_URL
driver.get(url)
time.sleep(5)
image = 'mge_map.png'
top_menu = driver.find_element(By.XPATH, '/html/body/div[1]/div[1]')
player_menu = driver.find_element(By.XPATH, '/html/body/div[1]/div[4]')
banner = driver.find_element(By.XPATH, '/html/body/div[1]/div[3]')
js_script = '''\
        arguments[0].style.display = 'none';
         '''
driver.execute_script(js_script, top_menu)
driver.execute_script(js_script, player_menu)
driver.execute_script(js_script, banner)
screenshot_area = driver.find_element(By.XPATH, '/html/body/div[1]/div[2]/div[2]/div/div/canvas')
with open(image, 'wb') as f:
    f.write(screenshot_area.screenshot_as_png)
c_image = Image.open(image)
width, height = c_image.size
new_size = (width//2, height//2)
resized_image = c_image.resize(new_size)
resized_image.save(image, optimize=False)
#im = pyimgur.Imgur(IMGUR_ID)
#uploaded_image = im.upload_image(image, title='mge_map').link
#print(uploaded_image)
#with open('map_imgur.txt', 'w') as f:
#    f.write(uploaded_image)


#image="mge_map_header.png"
#screenshot_area = driver.find_element(By.XPATH, '/html/body/div[1]/div[2]/div[1]')
#with open(image, 'wb') as f:
#    f.write(screenshot_area.screenshot_as_png)
#driver.close()
#uploaded_image = im.upload_image(image, title='mge_map_header').link
#print(uploaded_image)
#with open('map_header_imgur.txt', 'w') as f:
#    f.write(uploaded_image)
with open('map_update_time.txt', 'w') as f:
    f.write(dt_string)