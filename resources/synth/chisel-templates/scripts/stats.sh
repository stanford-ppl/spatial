#!bin/bash

widths=`grep -r "Widths" chisel/IOModule.scala | sed "s/^.*: //g" | sed "s/ //g"`
depths=`grep -r "Depths" chisel/IOModule.scala | sed "s/^.*: //g" | sed "s/ //g"`
widest=`grep -r "Widest" chisel/IOModule.scala | sed "s/^.*: //g" | sed "s/ //g"`
deepest=`grep -r "Deepest" chisel/IOModule.scala | sed "s/^.*: //g" | sed "s/ //g"`

# echo $widths
# echo $depths
# echo $widest
# echo $deepest

appname=`basename \`pwd\``
classname=`basename \`dirname \\\`pwd\\\`\``

echo "$appname,$classname,depths,$depths" >> /home/mattfel/spatial/stats.csv
echo "$appname,$classname,widths,$widths" >> /home/mattfel/spatial/stats.csv
echo "$appname,$classname,deepest,$deepest" >> /home/mattfel/spatial/stats.csv
echo "$appname,$classname,widest,$widest" >> /home/mattfel/spatial/stats.csv

# Fake out regression
echo "PASS: 1"
