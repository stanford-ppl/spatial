#!/bin/sh

if [ $# -eq 0 ]; then
    APP="Rendering3D"
else
    APP=$1
fi
rm -r gen/$APP
bin/spatial $APP
cd gen/$APP
sh run.sh 1 0 
cd ../../

