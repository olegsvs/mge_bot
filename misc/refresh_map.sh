#!/usr/bin/sh
nohup python3.10 map.py > logs/nohup_map.log &
sleep 20
cp -rf /home/bot/mge_bot/mge_map.png /home/www/static/mge_map.png
cp -rf /home/bot/mge_bot/mge_map_header.png /home/bot/reyohoho_static/mge_map_header.png