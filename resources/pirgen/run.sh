#!/bin/bash

args=$@
sbt "; set offline := true; runMain AccelMain $args"
