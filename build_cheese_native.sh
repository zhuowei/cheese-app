#!/bin/bash
set -e
(cd external/cheese; bash build.sh)
mkdir -p app/src/main/jniLibs/arm64-v8a
cp external/cheese/cheese app/src/main/jniLibs/arm64-v8a/libcheese.so
