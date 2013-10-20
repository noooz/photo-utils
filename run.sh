#!/bin/bash


wd=`pwd`

cd "$1"
dir=`pwd`

cd "$wd"
cd `dirname "$0"`

mvn exec:java -Dexec.args="$dir"