#!/bin/bash


wd=`pwd`/photo-rename

cd "$1"
dir=`pwd`

cd "$wd"
cd `dirname "$0"`

echo "scanning files in: $dir"
mvn compile exec:java -Dexec.args="'$dir' '$2'"
