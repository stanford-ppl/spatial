#!/bin/bash

# $1 = branch
# $2+ = args
bash run.sh $2 $3 $4 $5 $6 $7 $8 $9 | tee log

if [[ $GDOCS -eq 1 ]]; then

	pass_line=`cat log | grep "Assertion" | wc -l`

	if [[ ${pass_line} -gt 0 ]]; then
		pass=0
	else
		pass=1
	fi

	timeout_wc=`cat log | grep "TIMEOUT" | wc -l`
	runtime_string=`cat log | grep "Design ran for" | sed "s/Design ran for //g" | sed "s/ cycles.*//g"`

	if [[ ${timeout_wc} -gt 0 ]]; then
		runtime="TIMEOUT"
	else 
		runtime=$runtime_string
	fi

	# Hacky go back until $SPATIAL_HOME
	hash=`git rev-parse HEAD`
	ahash=nova-spatial
	#appname=`basename \`pwd\``
	appname=`cat chisel/IOModule_1.scala | grep "Root controller for app" | sed "s/.*: //g"`
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

	curpath=`pwd`
	basepath=`echo $curpath | sed "s/\/spatial\/.*/\/spatial\//g"`
	python3 ${basepath}/resources/regression/gdocs.py "report_regression_results" $1 $appname $pass $runtime $hash $ahash "$properties" "$2 $3 $4 $5 $6 $7 $8 $9"

fi

timeout=`if [[ $(cat log | grep TIMEOUT | wc -l) -gt 0 ]]; then echo 1; else echo 0; fi`
pass=`if [[ $(cat log | grep "Assertion" | wc -l) -gt 0 ]]; then echo 1; else echo 0; fi`

if [[ $pass = 0 && $timeout = 0 ]]; then
	exit 0
else
	exit 1
fi