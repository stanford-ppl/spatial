#!/bin/sh

if [ $# -eq 0 ]; then
    APP="Rendering3D"
else
    APP=$1
fi
rm -r gen/$APP
bin/spatial $APP --sim --vv --fpga ZCU 
cd gen/$APP
sh run.sh 0 0 
cd ../../

