#!/bin/bash

ARGC=$#  # Number of args, not counting $0
echo "$ARGC" > {{{INSERT_DESIGN_DIR}}}/arg_file.txt

i=1
while true; do
	if [ "$1" ]; then
		echo "$1" >> {{{INSERT_DESIGN_DIR}}}/arg_file.txt
		shift
	else
		break
	fi
	i=$((i+1))
done

cd {{{INSERT_DESIGN_DIR}}} && make C_TEST=test_spatial_main run
