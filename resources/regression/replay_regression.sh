#!/bin/bash

export GDOCS=1

for app in "$@"; do
  pushd gen/VCS/$app
  echo $app
  make
  args=`bash run.sh -h | grep Example | sed "s/.*run.sh //g"`
  bash scripts/regression_run.sh vcs $args
  popd
done
