#!/bin/bash

# $1 = branch
# $2+ = args
bash run.sh $2 $3 $4 $5 $6 $7 $8 $9 | tee log

if [[ $GDOCS -eq 1 ]]; then
	curpath=`pwd`
	basepath=`echo $curpath | sed "s/\/spatial\/.*/\/spatial\//g"`

	pass_line=`cat log | grep -a "Assertion" | wc -l`

	if [[ ${pass_line} -gt 0 ]]; then
		pass=N
	else
		pass=Y
	fi

	timeout_wc=`cat log | grep -a "TIMEOUT" | wc -l`
	runtime_string=`cat log | grep -a "Design ran for" | sed "s/Design ran for //g" | sed "s/ cycles.*//g" | tail -n1`
	scala_runtime_string=`cat log | grep -a "Total time: " | sed "s/.*Total time: //g" | sed "s/ s,.*//g" | tail -n1`

	if [[ ${timeout_wc} -gt 0 ]]; then
		runtime="TIMEOUT"
	elif [[ ! -z ${runtime_string} ]]; then
		runtime=$runtime_string
	else
		runtime=$scala_runtime_string
	fi

	# Get synthtime
	if [[ -f `pwd`/end.log ]]; then
	  endtime=`cat \`pwd\`/end.log`
	else
	  endtime=1
	fi
	if [[ -f `pwd`/start.log ]]; then
	  starttime=`cat \`pwd\`/start.log`
	else
	  starttime=0
	fi
	synthtime=$((endtime-starttime))

    # Get compile time
    logpath=`echo $curpath | sed "s/\/gen\//\/logs\//g"`
    if [[ -f $logpath/9999_Timing.log ]]; then
            spatialtime=`cat $logpath/9999_Timing.log | grep "compile:" | sed "s/.*compile: //g" | sed "s/s .*//g"`
    else
            spatialtime=0
    fi


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
		properties=`cat chisel/IOModule*.scala | grep "App Characteristics" | sed "s/^.*App Characteristics: //g" | sed "s/ //g"`
	else 
		appname=$(basename `pwd`)
		properties="NA"
	fi

	echo "${basepath}/resources/regression/gdocs.py \"report_regression_results\" $1 $appname $pass $runtime $hash $branchname $spatialtime $synthtime \"$properties\" \"$2 $3 $4 $5 $6 $7 $8 $9\""
	python3 ${basepath}/resources/regression/gdocs.py "report_regression_results" $1 $appname $pass $runtime $hash $branchname $spatialtime $synthtime "$properties" "$2 $3 $4 $5 $6 $7 $8 $9"

fi

timeout=`if [[ $(cat log | grep -a TIMEOUT | wc -l) -gt 0 ]]; then echo 1; else echo 0; fi`
pass=`if [[ $(cat log | grep -a "Assertion" | wc -l) -gt 0 ]]; then echo 1; else echo 0; fi`

if [[ $pass = 0 && $timeout = 0 ]]; then
	exit 0
else
	exit 1
fi
