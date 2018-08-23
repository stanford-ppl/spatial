#!/bin/bash

export NUM_THREADS=2

jobs=`ps aux | grep "mattfel.*sbt" | wc -l`
if [[ $jobs -gt 40 ]]; then
	echo "Too many sbt jobs running!  quitting..." > /tmp/last_synth
	exit 1
fi

if [[ $1 = "zynq" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export CLOCK_FREQ_MHZ=125
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/next-spatial/spatial/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "Zynq"`
elif [[ $1 = "zcu" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export CLOCK_FREQ_MHZ=100
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/next-spatial/spatial/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "ZCU"`
elif [[ $1 = "arria10" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export CLOCK_FREQ_MHZ=125
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/next-spatial/spatial/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "Arria10"`
elif [[ $1 = "aws" ]]; then
	export PIR_HOME=${REGRESSION_HOME}
	export CLOCK_FREQ_MHZ=250
	# Prep the spreadsheet
	cd ${REGRESSION_HOME}
	tid=`python3 ${REGRESSION_HOME}/next-spatial/spatial/resources/regression/gdocs.py "prepare_sheet" "$hash" "$branchname" "$timestamp" "AWS"`
fi

echo $tid > ${REGRESSION_HOME}/data/tid
echo $hash > ${REGRESSION_HOME}/data/hash
echo $branchname > ${REGRESSION_HOME}/data/branchname
ls ${REGRESSION_HOME}
ls ${REGRESSION_HOME}/next-spatial
echo $hash
echo $tid > ${REGRESSION_HOME}/next-spatial/spatial/tid
echo $hash > ${REGRESSION_HOME}/next-spatial/spatial/hash
echo $branchname > ${REGRESSION_HOME}/next-spatial/spatial/branchname

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
if [[ $tid = "-1" ]]; then
	sleep 3600 # Wait an hour
	rm -rf ${REGRESSION_HOME}/next-spatial
else 
	cd ${REGRESSION_HOME}
	rm -rf ${REGRESSION_HOME}/last-spatial
	mv ${REGRESSION_HOME}/current-spatial ${REGRESSION_HOME}/last-spatial
	mv ${REGRESSION_HOME}/next-spatial ${REGRESSION_HOME}/current-spatial
	export TEST_DATA_HOME=${REGRESSION_HOME}/current-spatial/test-data

	echo "Moving to ${REGRESSION_HOME}/spatial"
	cd ${REGRESSION_HOME}/current-spatial/spatial

	make install
	./tests.sh $1 ${REGRESSION_HOME}/current-spatial/spatial/regressions/${1}.list
fi
