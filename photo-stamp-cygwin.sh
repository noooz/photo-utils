#!/bin/bash

a1=$(realpath "$1")
a2=$(realpath "$2")

echo $a1

cd "`dirname "$0"`/photo-stamp"

echo "scanning files in: $a1"
mvn compile exec:java -Dexec.args="'$(cygpath -aw "$a1")' '$(cygpath -aw "$a2")'"
