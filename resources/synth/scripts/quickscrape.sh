#!bin/bash

#1 = Backend

if [[ $1 = "Zynq" ]]; then
	REGRESSION_HOME="/home/mattfel/sp_zynq/spatial"
elif [[ $1 = "ZCU" ]]; then
	REGRESSION_HOME="/home/mattfel/sp_zcu/spatial"
elif [[ $1 = "AWS" ]]; then
	REGRESSION_HOME="/home/mattfel/sp_aws/spatial"
elif [[ $1 = "AWS" ]]; then
	REGRESSION_HOME="/home/mattfel/sp_arria10/spatial"
fi

hash=`cat ${REGRESSION_HOME}/hash`
branchname=`cat ${REGRESSION_HOME}/branchname`

#appname=`basename \`pwd\``
fullname=`cat chisel/AccelWrapper*.scala | grep "Root controller for app" | sed "s/.*: //g"`
aws_dir_name=`basename \`pwd\``
testdirs=`find ${REGRESSION_HOME}/test -type d -printf '%d\t%P\n' | sort -r -nk1 | cut -f2- | grep -v target | sed "s/.*\///g"`
testdirsarray=($testdirs)
for t in "${testdirsarray[@]}"; do
	fullname=`echo $fullname | sed "s/${t}_//g" | sed "s/${t}\.//g"`
done
appname=$fullname
if [[ $1 = "Zynq" ]]; then
	par_util=`pwd`/verilog-zynq/par_utilization.rpt
	if [[ ! -f ${par_util} ]]; then par_util=`pwd`/verilog-zynq/synth_utilization.rpt; fi
	par_tmg=`pwd`/verilog-zynq/par_timing_summary.rpt
	if [[ ! -f ${par_tmp} ]]; then par_tmp=`pwd`/verilog-zynq/synth_timing_summary.rpt; fi
	word="Slice"
	f1=3
	f2=6
elif [[ $1 = "ZCU" ]]; then
    par_util=`pwd`/verilog-zcu/par_utilization.rpt
    if [[ ! -f ${par_util} ]]; then par_util=`pwd`/verilog-zcu/synth_utilization.rpt; fi
    par_tmg=`pwd`/verilog-zcu/par_timing_summary.rpt
    if [[ ! -f ${par_tmp} ]]; then par_tmp=`pwd`/verilog-zcu/synth_timing_summary.rpt; fi
    word="CLB"
    f1=3
    f2=6
elif [[ $1 = "Arria10" ]]; then
    par_util=`pwd`/verilog-zcu/par_utilization.rpt      # TODO: Tian
    par_tmg=`pwd`/verilog-zcu/par_timing_summary.rpt    # TODO: Tian
    word="CLB"                                          # TODO: Tian
    f1=3                                                # TODO: Tian
    f2=6                                                # TODO: Tian
elif [[ $1 = "AWS" ]]; then
	par_util=/home/mattfel/aws-fpga/hdk/cl/examples/${aws_dir_name}/build/reports/utilization_route_design.rpt
	par_tmg=/home/mattfel/aws-fpga/hdk/cl/examples/${aws_dir_name}/build/reports/timing_summary_route_design.rpt
	word="CLB"
	f1=5
	f2=8
fi

if [[ -f ${par_util} ]]; then
	lutraw=`cat $par_util | grep -m 1 "$word LUTs" | awk -v f=$f1 -F'|' '{print $f}' | sed "s/ //g"`
	lutpcnt=`cat $par_util | grep -m 1 "$word LUTs" | awk -v f=$f2 -F'|' '{print $f}' | sed "s/ //g"`
	regraw=`cat $par_util | grep -m 1 "$word Registers" | awk -v f=$f1 -F'|' '{print $f}' | sed "s/ //g"`
	regpcnt=`cat $par_util | grep -m 1 "$word Registers" | awk -v f=$f2 -F'|' '{print $f}' | sed "s/ //g"`
	ramraw=`cat $par_util | grep -m 1 "| Block RAM Tile" | awk -v f=$f1 -F'|' '{print $f}' | sed "s/ //g"`
	rampcnt=`cat $par_util | grep -m 1 "| Block RAM Tile" | awk -v f=$f2 -F'|' '{print $f}' | sed "s/ //g"`
	if [[ $1 = "AWS" ]]; then
		uramraw=`cat $par_util | grep -m 1 "URAM" | awk -v f=$f1 -F'|' '{print $f}' | sed "s/ //g"`
		urampcnt=`cat $par_util | grep -m 1 "URAM" | awk -v f=$f2 -F'|' '{print $f}' | sed "s/ //g"`
	else
		uramraw="NA"
		urampcnt="NA"
	fi
	dspraw=`cat $par_util | grep -m 1 "DSPs" | awk -v f=$f1 -F'|' '{print $f}' | sed "s/ //g"`
	dsppcnt=`cat $par_util | grep -m 1 "DSPs" | awk -v f=$f2 -F'|' '{print $f}' | sed "s/ //g"`
	lalraw=`cat $par_util | grep -m 1 "LUT as Logic" | awk -v f=$f1 -F'|' '{print $f}' | sed "s/ //g"`
	lalpcnt=`cat $par_util | grep -m 1 "LUT as Logic" | awk -v f=$f2 -F'|' '{print $f}' | sed "s/ //g"`
	lamraw=`cat $par_util | grep -m 1 "LUT as Memory" | awk -v f=$f1 -F'|' '{print $f}' | sed "s/ //g"`
	lampcnt=`cat $par_util | grep -m 1 "LUT as Memory" | awk -v f=$f2 -F'|' '{print $f}' | sed "s/ //g"`
else
	lutraw="NA"
	lutpcnt="NA"
	regraw="NA"
	regpcnt="NA"
	ramraw="NA"
	rampcnt="NA"
	uramraw="NA"
	urampcnt="NA"
	dspraw="NA"
	dsppcnt="NA"
	lalraw="NA"
	lalpcnt="NA"
	lamraw="NA"
	lampcnt="NA"
fi

if [[ -f ${par_tmg} ]]; then
	viocnt=`cat $par_tmg | grep -i violated | grep -v synth | wc -l`
	if [[ $viocnt != "0" ]]; then tmg="0"; else tmg="1"; fi
else
	tmg="NA"
fi


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

echo "LUT: $lutraw (${lutpcnt}%) Regs: $regraw (${regpcnt}%) BRAM: $ramraw (${rampcnt}%) URAM: $uramraw (${urampcnt}%) DSP: $dspraw (${dsppcnt}%) LaL: $lalraw (${lalpcnt}%) LaM: $lamraw (${lampcnt}%) Synthtime: $synthtime Tmg_Met: $tmg $1"
