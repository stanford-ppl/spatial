#!/bin/bash

#export NUM_THREADS=2
export REGRESSION_HOME=`pwd`

export branchname=`git rev-parse --abbrev-ref HEAD | sed "s/HEAD/unknown/g"`
export hash=`git rev-parse HEAD`
export timestamp=`git show -s --format=%ci`

jobs=`ps aux | grep "mattfel.*sbt" | wc -l`
if [[ $jobs -gt 40 ]]; then
	echo "Too many sbt jobs running!  quitting..." > /tmp/last_synth
	exit 1
fi

if [[ $1 = "zynq" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export KEEP_HIERARCHY=1
	export CLOCK_FREQ_MHZ=125
	export FRINGE_PACKAGE="zynq"
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "Zynq"`
elif [[ $1 = "zcu" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export CLOCK_FREQ_MHZ=100
	export KEEP_HIERARCHY=1
	export FRINGE_PACKAGE="zcu"
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "ZCU"`
elif [[ $1 = "arria10" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export CLOCK_FREQ_MHZ=125
	export FRINGE_PACKAGE="arria10"
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "Arria10"`
elif [[ $1 = "aws" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export CLOCK_FREQ_MHZ=250
	export KEEP_HIERARCHY=1
	export FRINGE_PACKAGE="aws"
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "AWS"`
fi

echo $tid > ${REGRESSION_HOME}/../data/tid
echo $hash > ${REGRESSION_HOME}/../data/hash
echo $branchname > ${REGRESSION_HOME}/../data/branchname
#ls ${REGRESSION_HOME}
#ls ${REGRESSION_HOME}/next-spatial
echo $hash
echo $tid > ${REGRESSION_HOME}/tid
echo $hash > ${REGRESSION_HOME}/hash
echo $branchname > ${REGRESSION_HOME}/branchname

export PATH=/usr/bin:/local/ssd/home/mattfel/aws-fpga/hdk/common/scripts:/opt/Xilinx/SDx/2017.1/Vivado/bin:/opt/Xilinx/SDx/2017.1/SDK/bin:/opt/Xilinx/Vivado/2017.1/bin:/opt/Xilinx/SDK/2017.1/bin:$PATH
export LM_LICENSE_FILE=1717@cadlic0.stanford.edu:7195@cadlic0.stanford.edu:7193@cadlic0.stanford.edu:/opt/Xilinx/awsF1.lic:27000@cadlic0.stanford.edu:$LM_LICENSE_FILE
export VCS_HOME=/cad/synopsys/vcs/K-2015.09-SP2-7
export QVER=17.1
alias qsys=/opt/intelFPGA_pro/$QVER/qsys/bin/qsys-edit
export PATH=/usr/bin:$VCS_HOME/amd64/bin:/opt/intelFPGA_pro/$QVER/quartus/bin:$PATH
export QSYS_ROOTDIR=/opt/intelFPGA_pro/$QVER/qsys/bin/
export PATH=/opt/intelFPGA_pro/$QVER/quartus/sopc_builder/bin:$PATH
export ALTERAOCLSDKROOT=/opt/intelFPGA_pro/$QVER/hld
export LM_LICENSE_FILE=/opt/intelFPGA_pro/licenses/arria10-license.dat:$LM_LICENSE_FILE
export ALTERAD_LICENSE_FILE=/opt/intelFPGA_pro/licenses/arria10-license.dat
export USING_THIS_QUARTUS=`which quartus`

# Current hash matches previous hash, skip test
cd ${REGRESSION_HOME}
mv ${REGRESSION_HOME}/gen ${REGRESSION_HOME}/prev_gen
export TEST_DATA_HOME=/home/mattfel/test-data


make install
./test.sh $1 ${REGRESSION_HOME}/regressions/${1}.list
