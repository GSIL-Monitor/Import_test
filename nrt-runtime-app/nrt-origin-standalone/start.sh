#!/usr/bin/env bash

JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
    JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8010,server=y,suspend=n "
fi
#nohup java  $JAVA_DEBUG_OPTS -jar nrt-origin-standalone-1.0-SNAPSHOT.jar > log 2>&1 &
java  $JAVA_DEBUG_OPTS -jar nrt-origin-standalone-1.0-SNAPSHOT.jar