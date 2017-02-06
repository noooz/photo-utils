#!/bin/bash

wd=`pwd`/photo-stamp

cd "$1"
dir=`pwd`

cd "$wd"
cd `dirname "$0"`

echo "scanning files in: $dir"
mvn compile exec:java -Dexec.args="'$(cygpath -aw "$dir")' '$2'"
