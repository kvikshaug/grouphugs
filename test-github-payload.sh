#!/usr/bin/env bash
# main() in GithubPostReceiveServer reads from stdin. pipe json into it to parse it with gson
pushd "$(dirname $0)"
java -cp target/grouphug-1.0-SNAPSHOT.jar no.kvikshaug.gh.GithubPostReceiveServer "$@"
popd
