#!/bin/bash

echo "Run this script with a space-separated list of apps you want to rerun as the arguments"
echo "REMEMBER TO CHANGE THE HOST COLUMN OF THE SPREADSHEET TO WHATEVER SERVER YOU ARE RUNNING ON (IT WON'T UPDATE THE SHEET IF THIS DOESN'T MATCH"
export GDOCS=1

for app in "$@"; do
  pushd gen/VCS/$app
  echo $app
  make
  args=`bash run.sh -h | grep Example | sed "s/.*run.sh //g"`
  bash scripts/regression_run.sh vcs $args
  popd
done
