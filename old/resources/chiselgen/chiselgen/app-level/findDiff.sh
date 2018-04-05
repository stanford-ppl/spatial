#!/bin/bash

if [ $# -lt 1 ]; then
  echo "Usage: $0 <directory_to_check>"
  exit -1
fi

DIR=$1

SRCS=$(find . -iname $DIR)
for src in $SRCS; do
  BACKEND=$(echo $src | cut -f2 -d'/')
  diff -r -q $src $(find $SPATIAL_HOME/spatial/core/resources/${BACKEND}gen -iname $DIR) | cut -f2,4 -d' '
done

