#!/bin/bash
set -e
if [ -e "/tmp/gh.pid" ]
then
    kill "$(cat /tmp/gh.pid)" # kill bot
fi

./link-conf.sh
./link-db.sh

pushd "$(dirname $0)/target" # make sure we're in the correct directory
java -jar gh-1.0.0.jar > gh.log 2>&1 & # restart bot
echo "$!" > /tmp/gh.pid # save pid
popd
