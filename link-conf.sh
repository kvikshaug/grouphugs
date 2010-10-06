#!/bin/bash
set -e
pushd "$(dirname $0)" # make sure we're in the correct directory

pushd target
ln -s ../props.xml props.xml # symlink props.xml
popd
