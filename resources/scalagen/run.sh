#!/bin/bash

args=$@
sbt -batch "run $args"
