#!/bin/bash

a1=$(realpath "$1")
a2=$(realpath "$2")

echo "delete extraneous files in '$a2'"
rsync -r --delete --ignore-non-existing --ignore-existing "$a1/" "$a2/"

echo "scanning files in '$a1'"

if which cygpath 2>/dev/null; then 
    a1=$(cygpath -aw "$a1")
    a2=$(cygpath -aw "$a2")
fi

cd "`dirname "$0"`/photo-stamp"
mvn compile exec:java -Dexec.args="'$a1' '$a2'"
