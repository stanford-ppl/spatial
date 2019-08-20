#!/usr/bin/env bash
echo $1

rm -rf gen/$1 && bin/spatial $1 --fpga=vcs --synth --instrumentation && cd gen/$1 && make && bash run.sh | tee run.log && cd ../../