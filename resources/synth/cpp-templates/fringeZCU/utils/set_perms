#!/bin/bash

if [ $# -ne 1 ]; then
  echo "Usage: $0 <file>"
  exit -1
fi

FILE=`readlink -f $1`
echo $FILE
sudo chown 0:0 $FILE
sudo chmod u+s $FILE

echo "Done"

