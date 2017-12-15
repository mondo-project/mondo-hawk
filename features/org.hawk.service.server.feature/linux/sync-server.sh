#!/bin/bash

if (( "$#" < 1 || "$#" > 2 )); then
    cat 1>&2 <<EOF
Requests an immediate reindex on an index hosted by the Hawk server, without
waiting for it to be completed. This script is designed to be used from UNIX
cron-style task schedulers, without imposing additional dependencies or
configuration.

Usage: [HOST=localhost|PORT=8080|PROTOCOL=http]* $0 (hawk-instance) [curlconfig.cfg]

Notation is [optional] and (mandatory). You can use authentication through the
use of a curl config file. For instance, this file would provide the credentials
for authentication if security is enabled in your Hawk instance:

  user=youruser:yourpassword

For security reasons, please make sure the curlconfig.cfg file is only readable
by the user running this script.

This script requires curl and Python 2.6+.

EOF
    exit 1
fi

request_failed() {
    JSON_REPLY="$1"

    # hawkSyncInstance does not return anything: any field is an indication of failure.
    SIZE_FIELDS=$(python -c 'import sys, json; print len(json.load(sys.stdin)[4])' <<<"$JSON_REPLY")

    test "$SIZE_FIELDS" != 0
}

HOST="${HOST:-localhost}"
PORT="${PORT:-8080}"
PROTOCOL="${PROTOCOL:-http}"

HAWK_INSTANCE="$1"
CURL_CONFIG="$2"
shift 2

# Structure of message is:
#
#   [JSON protocol version (1), method name, message type (1 - call),
#    sequence ID (1 = first message),
#    {argid: {argtype: argvalue}...}]
#
# Here, "str" means "string argument", and "tf" means "boolean argument".

JSON_MESSAGE="$(cat <<EOF
[1,"syncInstance",1,1,{"1":{"str":"$HAWK_INSTANCE"},"2":{"tf":0}}]
EOF
)"

if [[ "z$CURL_CONFIG" == "z" ]]; then
    JSON_REPLY=$(curl -s -d@- "$PROTOCOL://$HOST:$PORT/thrift/hawk/json" <<<"$JSON_MESSAGE")
else
    JSON_REPLY=$(curl -s -d@- "$PROTOCOL://$HOST:$PORT/thrift/hawk/json" -K "$CURL_CONFIG" <<<"$JSON_MESSAGE")
fi

if request_failed "$JSON_REPLY"; then
    echo 1>&2 "Request failed: full message is '$JSON_REPLY'"
    exit 2
fi
