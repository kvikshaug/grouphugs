#!/usr/bin/env bash
set -e
PIDFILE=gh.pid
LOGFILE=gh.log

function startbot() {
    java -jar target/grouphug-1.0-SNAPSHOT.jar > $LOGFILE 2>&1 &
    echo "$!" > $PIDFILE # save pid
}

function stopbot() {
    if [ -e "$PIDFILE" ]
    then
        kill "$(cat $PIDFILE)" # kill bot
        rm "$PIDFILE"
    fi
}

function buildbot() {
    mvn clean compile package
}

function testgithubpayload() {
    java -cp target/grouphug-1.0-SNAPSHOT.jar no.kvikshaug.gh.github.GithubPostReceiveServer
}

function pullfrommaster() {
    git pull murr4y master
}

function usage() {
    echo "$0 [start|stop|build|restart|rebuild] or any combination of those"
    echo "start: starts the bot"
    echo "stop: stops the bot"
    echo "build: recompiles the bot (not recommended while the bot is running)"
    echo "restart: restarts the bot"
    echo "rebuild: stops, recompiles, and starts the bot again"
    echo "upgrade: stops, pulls from murr4y master, recompiles, and starts the bot again"
    echo "test-payload: tries to parse a Github payload from stdin"
}

cd "$(dirname $0)"

if [ $# -lt 1 ]
then
    usage
    exit 1
fi

for arg in "$@"
do
    case "$arg" in
        "start")
            startbot
            ;;

        "stop")
            stopbot
            ;;
        "build")
            buildbot
            ;;

        "restart")
            stopbot
            startbot
            ;;

        "rebuild")
            stopbot
            buildbot
            startbot
            ;;

        "test-payload")
            testgithubpayload
            ;;

        "upgrade")
            stopbot
            pullfrommaster
            buildbot
            startbot
            ;;

        *)
            usage
            exit 1
    esac
 done
