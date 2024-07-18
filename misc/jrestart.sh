#!/usr/bin/sh
pkill -f mge_bot.jar
pkill -f HowLongToBeat-Proxy-API-1.0-SNAPSHOT.jar
sleep 3
nohup python3.10 refresh.py >> logs/nohup_refresh.log &
sleep 5
nohup java -jar HowLongToBeat-Proxy-API-1.0-SNAPSHOT.jar >> logs/hltb.log &
nohup java -jar mge_bot.jar >> logs/nohup_java_log.log &