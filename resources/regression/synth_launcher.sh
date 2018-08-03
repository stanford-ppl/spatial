#!/bin/bash

#1 = backend

export LANG=en_US.UTF-8
export JAVA_HOME=$(readlink -f $(dirname $(readlink -f $(which java)))/..)
if [[ ${JAVA_HOME} = *"/jre"* ]]; then # ugly hack because idk what is going on with tucson
  export JAVA_HOME=${JAVA_HOME}/..
fi
export REGRESSION_ENV=1
export XILINX_VIVADO=/opt/Xilinx/Vivado/2017.1
export PATH=/usr/bin:/local/ssd/home/mattfel/aws-fpga/hdk/common/scripts:/opt/Xilinx/SDx/2017.1/Vivado/bin:/opt/Xilinx/SDx/2017.1/SDK/bin:$PATH
export FOREGROUND="-foreground"
#cd /home/mattfel/aws-fpga/
inputarg=$1
if [[ $inputarg = "aws" ]]; then
	set -- "${@:2}" # unset the damn args
	source /home/mattfel/aws-fpga/hdk_setup.sh 
fi
export LD_LIBRARY_PATH=${XILINX_VIVADO}/lib/lnx64.o:${LD_LIBRARY_PATH}
export AWS_HOME=/home/mattfel/aws-fpga
export AWS_CONFIG_FILE=/home/mattfel/aws-fpga/hdk/cl/examples/rootkey.csv
export RPT_HOME=/home/mattfel/aws-fpga/hdk/cl/examples

this_machine=`hostname`
export SBT_OPTS="-Xmx32G -Xss1G"
export _JAVA_OPTIONS="-Xmx32g -Xss8912k -Xms16g"
export REGRESSION_HOME="/home/mattfel/regression/synth/$inputarg"

export SPATIAL_HOME=${REGRESSION_HOME}/current-spatial/

if [[ ! -f ${REGRESSION_HOME}/lock ]]; then
	touch ${REGRESSION_HOME}/lock
	rm ${REGRESSION_HOME}/protocol/done
	rm -rf ${REGRESSION_HOME}/next-spatial/
	mkdir ${REGRESSION_HOME}/next-spatial
	cd ${REGRESSION_HOME}/next-spatial
	git clone git@github.com:stanford-ppl/spatial
	git clone git@github.com:stanford-ppl/test-data.git
	cd spatial
	export 
	export branchname=`git rev-parse --abbrev-ref HEAD | sed -i "s/HEAD/unknown/g"`
	export hash=`git rev-parse HEAD`
	export timestamp=`git show -s --format=%ci`
	echo $hash > ${REGRESSION_HOME}/data/hash
	echo $branchname > ${REGRESSION_HOME}/data/branchname
	echo $timestamp > ${REGRESSION_HOME}/data/timestamp

	# Run tests
	cd ${REGRESSION_HOME}/next-spatial/spatial
	set $inputarg
	echo "Running synth_regression with $inputarg"
	bash resources/regression/synth_regression.sh $inputarg

	rm ${REGRESSION_HOME}/lock
	touch ${REGRESSION_HOME}/protocol/done
fi