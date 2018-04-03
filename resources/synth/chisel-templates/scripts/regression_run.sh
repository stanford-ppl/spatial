#!/bin/bash

# $1 = branch
# $2+ = args
bash run.sh $2 $3 $4 $5 $6 $7 $8 $9 | tee log

if [[ $REGRESSION_ENV -eq 1 ]]; then

	pass_line=`cat log | grep "PASS"`

	if [[ ${pass_line} = *": 1"* ]]; then
		pass=1
	elif [[ ${pass_line} = *": 0"* ]]; then
		pass=0
	else
		pass="?"
	fi

	timeout_wc=`cat log | grep "TIMEOUT" | wc -l`
	runtime_string=`cat log | grep "Design ran for" | sed "s/Design ran for //g" | sed "s/ cycles.*//g"`

	if [[ ${timeout_wc} -gt 0 ]]; then
		runtime="TIMEOUT"
	else 
		runtime=$runtime_string
	fi

	# Hacky go back until $SPATIAL_HOME
	hash=`cat ../../../../hash`
	ahash=`cat ../../../../ahash`
	appname=`basename \`pwd\``
	properties=`cat chisel/IOModule.scala | grep "App Characteristics" | sed "s/^.*App Characteristics: //g" | sed "s/ //g"`

	if [[ $1 = "Zynq" ]]; then
		REGRESSION_HOME="/home/mattfel/regression/synth/zynq"
	elif [[ $1 = "ZCU" ]]; then
		REGRESSION_HOME="/home/mattfel/regression/synth/zcu"
	elif [[ $1 = "AWS" ]]; then
		REGRESSION_HOME="/home/mattfel/regression/synth/aws"
	elif [[ $1 = "Arria10" ]]; then
		REGRESSION_HOME="/home/mattfel/regression/synth/arria10"
	fi

	python3 ../../../../utilities/gdocs.py "report_regression_results" $1 $appname $pass $runtime $hash $ahash "$properties" "$2 $3 $4 $5 $6 $7 $8 $9"

fi