#!/bin/bash

./set_perms Top
./Top $@ 2>&1 | tee run.log
