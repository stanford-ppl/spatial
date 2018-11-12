#!/bin/sh

if [ $# -eq 0 ]; then
    APP="Rendering3D"
else
    APP=$1
fi


rm -r gen/$APP
bin/spatial $APP --fpga=VCS --dot --instrument
cd gen/$APP
make 
bash run.sh 18000 2000 0
