#!/bin/sh
cd app/src/main/assets/cheese
rm magisk.apk busybox
curl -L https://github.com/topjohnwu/Magisk/releases/download/v29.0/Magisk-v29.0.apk >magisk.apk
7z x magisk.apk lib/arm64-v8a/libbusybox.so
mv lib/arm64-v8a/libbusybox.so busybox
rm -r lib/arm64-v8a
