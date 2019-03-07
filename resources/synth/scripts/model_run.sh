#!/bin/bash

# $1 = branch
# $2 = version
# $3+ = args
cmd='make $2-model ARGS="noninteractive $3 $4 $5 $6 $7 $8 $9" | tee log_$2'
eval $cmd

if [[ $GDOCS -eq 1 && $2 = "final" ]]; then
	curpath=`pwd`
	basepath=`echo $curpath | sed "s/\/spatial\/.*/\/spatial\//g"`

	true_runtime_string=`cat log | grep -a "Design ran for" | sed "s/Design ran for //g" | sed "s/ cycles.*//g" | tail -n1`
	dse_runtime_string=`cat log_dse | grep -a "Total Cycles for App" | sed "s/.*Total Cycles for App.*: //g" | tail -n1`
	final_runtime_string=`cat log_final | grep -a "Total Cycles for App" | sed "s/.*Total Cycles for App.*: //g" | tail -n1`

	# Hacky go back until $SPATIAL_HOME
	hash=`cat ${basepath}/reghash`
	branchname=`cat ${basepath}/branchname`
	#appname=`basename \`pwd\``
	if [[ -d chisel ]]; then
		fullname=`cat chisel/IOModule*.scala | grep "Root controller for app" | sed "s/.*: //g"`
		testdirs=`find ${basepath}/test -type d -printf '%d\t%P\n' | sort -r -nk1 | cut -f2- | grep -v target | sed "s/.*\///g"`
		testdirsarray=($testdirs)
		for t in "${testdirsarray[@]}"; do
			fullname=`echo $fullname | sed "s/${t}_//g" | sed "s/${t}\.//g"`
		done
		appname=$fullname
	else 
		appname=$(basename `pwd`)
	fi

	echo "${basepath}/resources/regression/gdocs.py \"report_model_results\" $1 $appname $hash $branchname ${true_runtime_string} ${dse_runtime_string} ${final_runtime_string} \"$3 $4 $5 $6 $7 $8 $9\""
	python3 ${basepath}/resources/regression/gdocs.py "report_model_results" $1 $appname $hash $branchname ${true_runtime_string} ${dse_runtime_string} ${final_runtime_string} "$3 $4 $5 $6 $7 $8 $9"

fi

exit 1
