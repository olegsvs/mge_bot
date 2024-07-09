#!/usr/bin/sh
nohup python3.10 map.py > logs/nohup_map.log &
sleep 35
FILENAME=mge_map.png
SIZE=$(du -sb $FILENAME | awk '{ print $1 }')

if ((SIZE<890000)) ; then
    echo "less, need retry...";
else
    echo "not less, cp";
    cp -rf /home/bot/mge_bot/mge_map.png /home/www/static/mge_map.png
    cp -rf /home/bot/mge_bot/mge_map_header.png /home/www/static/mge_map_header.png
    cp -rf /home/bot/mge_bot/map_update_time_new.txt /home/bot/mge_bot/map_update_time.txt
fi
