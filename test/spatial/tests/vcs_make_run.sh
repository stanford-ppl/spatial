#!/bin/sh

if [ $# -eq 0 ]; then
    APP="Rendering3D"
else
    APP=$1
fi


rm -r gen/$APP
bin/spatial $APP --synth --syncMem --retime 
cd gen/$APP
make vcs
bash run.sh 1 0
