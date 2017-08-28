#!/bin/bash



args=("$@") 

for ((i=0; i<${#args[@]}; i++)); do
    p=${args[$i]}
    #echo $p
    if [[ "$p" == *"/"* ]]; then
	args[$i]=$(cygpath -aw "$p")
	echo "'$p' -> '${args[$i]}'"
    fi
done


echo "${args[@]}"

"${args[@]}"


