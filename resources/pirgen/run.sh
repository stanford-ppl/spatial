#!/bin/bash

args=$@
sbt "; runMain AccelMain $args"
