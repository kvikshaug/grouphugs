#!/usr/bin/env bash
set -e
PIDFILE=gh.pid
LOGFILE=gh.log

function start() {
    java -cp "$(cat classpath):grouphug-1.0-SNAPSHOT.jar" no.kvikshaug.gh.Grouphug > $LOGFILE 2>&1 &
    echo "$!" > $PIDFILE # save pid
}

function stop() {
    if [ -e "$PIDFILE" ]
    then
        kill "$(cat $PIDFILE)" # kill bot
        rm "$PIDFILE"
    fi
}

function testgprspayload() {
    java -cp "$(cat classpath):grouphug-1.0-SNAPSHOT.jar" no.kvikshaug.gh.github.GithubPostReceiveServer
}

function build() {
    pushd ..
    sbt compile mkrunnable
    popd
}

function pull() {
    pushd ..
    git pull
    popd
}

function usage() {
    echo "$0 [start|stop|build|restart|rebuild|test-payload] or any combination of those"
    echo "start: starts the bot"
    echo "stop: stops the bot"
    echo "build: recompiles the bot (not recommended while the bot is running)"
    echo "restart: restarts the bot"
    echo "rebuild: stops, recompiles, and starts the bot again"
    echo "upgrade: stops, pulls from tracked remotes, recompiles, and starts the bot again"
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
            start
            ;;

        "stop")
            stop
            ;;
        "build")
            build
            ;;

        "restart")
            stop
            start
            ;;

        "rebuild")
            stop
            build
            start
            ;;

        "test-payload")
            testgprspayload
            ;;

        "upgrade")
            stop
            pull
            build
            start
            ;;

        *)
            usage
            exit 1
    esac
 done
