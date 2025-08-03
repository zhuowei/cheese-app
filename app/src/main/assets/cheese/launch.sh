#!/bin/sh
echo "Success!"
cd "$(dirname "$0")"
export FIRST_STAGE=1
export ASH_STANDALONE=1
#exec ./busybox ./live_setup.sh
setsid sleep 100000
runcon u:r:init:s0 stop
