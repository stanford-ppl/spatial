#!/bin/bash
if [[ "$USE_IDEAL_DRAM" = "0" || "$USE_IDEAL_DRAM" = "1" ]]; then
	ideal=$USE_IDEAL_DRAM
else
	ideal=0
fi
if [[ "$DEBUG_REGS" = "0" || "$DEBUG_REGS" = "1" ]]; then
	dbg_reg=$USE_IDEAL_DRAM
else
	dbg_reg=0
fi
if [[ "$DEBUG_DRAM" = "0" || "$DEBUG_DRAM" = "1" ]]; then
	dbg_dram=$USE_IDEAL_DRAM
else
	dbg_dram=0
fi
export USE_IDEAL_DRAM=$ideal
export DRAM_DEBUG=$dbg_dram
export DEBUG_REGS=$dbg_reg
export VPD_ON=0
export VCD_ON=0
export DRAM_NUM_OUTSTANDING_BURSTS=-1  # -1 == infinite number of outstanding bursts
export DRAMSIM_HOME=`pwd`/verilog/DRAMSim2
export LD_LIBRARY_PATH=${DRAMSIM_HOME}:$LD_LIBRARY_PATH


export N3XT_LOAD_DELAY=3
export N3XT_STORE_DELAY=11
export N3XT_NUM_CHANNELS=64

export APP_NAME=`basename $PWD`

./Top $@ 2>&1 | tee sim.log

if [[ -e verilog-aws-sim ]]; then 
  mkdir waves
  cd waves
  cp ${AWS_HOME}/hdk/cl/examples/${APP_NAME}/verif/sim/test_spatial_main/Top.vcd .
  cp ../verilog-aws-sim/Top.v . 
  cp ../verilog-aws-sim/aws_tb_dummy.flist . 
  cp ../verilog-aws-sim/aws_tb_dummy.v . 
  cd ..
  tar -cvjf ${APP_NAME}_waves.tar.gz waves
fi

if [[ "$USE_IDEAL_DRAM" = "1" ]]; then
	echo "Ideal DRAM Simulation"
elif [[ "$USE_IDEAL_DRAM" = "0" ]]; then
	echo "Realistic DRAM Simulation"
else
	echo "UNKNOWN DRAM SIMULATION!"
fi
