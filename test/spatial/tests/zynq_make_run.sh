#!/bin/sh

if [ $# -eq 0 ]; then
    APP="Rendering3D"
else
    APP=$1
fi


rm -r gen/$APP
bin/spatial $APP --synth --syncMem --retime --instrumentation
cd gen/$APP
make zcu 
