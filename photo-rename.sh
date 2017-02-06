#!/bin/bash

dir=$(realpath "$1")

echo "scanning files in: $dir"

if which cygpath 2>/dev/null; then 
    dir=$(cygpath -aw "$dir")
fi

cd "`dirname "$0"`/photo-rename"
mvn compile exec:java -Dexec.args="'$dir' '$2'"
