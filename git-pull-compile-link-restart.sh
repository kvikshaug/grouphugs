#!/bin/bash
set -e
pushd "$(dirname $0)" # make sure we're in the correct directory

git pull "$@" # pull changes from a git remote
buildr clean compile package # recompile, package

./link-db.sh
./link-conf.sh
./restart-bot.sh
popd
