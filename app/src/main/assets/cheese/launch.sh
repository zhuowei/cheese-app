#!/bin/sh
echo "Success! Starting Magisk..."
cd "$(dirname "$0")"
umask 000
export FIRST_STAGE=1
export ASH_STANDALONE=1
exec ./busybox setsid ./busybox nsenter -m/proc/1/ns/mnt "$PWD/busybox" sh "$PWD/live_setup.sh" >/data/local/tmp/cheese_magisk_start.txt 2>&1
