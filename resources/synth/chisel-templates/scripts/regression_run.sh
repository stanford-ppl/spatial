#!/bin/bash

# $1 = branch
# $2+ = args
bash run.sh $2 $3 $4 $5 $6 $7 $8 $9 | tee log

if [[ $GDOCS -eq 1 ]]; then
	curpath=`pwd`
	basepath=`echo $curpath | sed "s/\/spatial\/.*/\/spatial\//g"`

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
	fullname=`cat chisel/IOModule_1.scala | grep "Root controller for app" | sed "s/.*: //g"`
	testdirs=`find ${basepath}/test -type d -printf '%d\t%P\n' | sort -r -nk1 | cut -f2- | grep -v target | sed "s/.*\///g"`
	testdirsarray=($testdirs)
	for t in "${testdirsarray[@]}"; do
		fullname=`echo $fullname | sed "s/${t}_//g"`
	done
	appname=$fullname
	properties=`cat chisel/IOModule_1.scala | grep "App Characteristics" | sed "s/^.*App Characteristics: //g" | sed "s/ //g"`

	python3 ${basepath}/resources/regression/gdocs.py "report_regression_results" $1 $appname $pass $runtime $hash $ahash "$properties" "$2 $3 $4 $5 $6 $7 $8 $9"

fi

timeout=`if [[ $(cat log | grep TIMEOUT | wc -l) -gt 0 ]]; then echo 1; else echo 0; fi`
pass=`if [[ $(cat log | grep "Assertion" | wc -l) -gt 0 ]]; then echo 1; else echo 0; fi`

if [[ $pass = 0 && $timeout = 0 ]]; then
	exit 0
else
	exit 1
fi