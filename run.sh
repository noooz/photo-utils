#!/bin/bash


dir=`pwd`
cd `dirname "$0"`

mvn exec:java -Dexec.workingdir="$dir" -Dexec.args="$1"