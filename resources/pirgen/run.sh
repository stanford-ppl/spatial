#!/bin/bash

args=$@
sbt "; runMain AccelMain $args"
python3 bin/simstat.py
python3 bin/annotate.py
