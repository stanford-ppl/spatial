TOP=Top
EXE=accel.bit.bin
CC=g++

# calculate CLOCK_PERIOD based on CLOCK_FREQ_MHZ (what if it is not defined?)
CLOCK_PERIOD=$(shell expr 1000 / ${CLOCK_FREQ_MHZ} / 2)


# Option for setting random seed: +ntb_random_seed=<number>
VCS_OPTS=-full64 -quiet -timescale=1ns/1ps -sverilog -debug_pp -Mdir=${TOP}.csrc +v2k +vcs+lic+wait +vcs+initreg+random +define+CLOCK_PERIOD=${CLOCK_PERIOD} +lint=TFIPC-L +libext++.v 
CC_OPTS=-LDFLAGS "-L../ -ldramsim -lstdc++ -Wl,-rpath=../" -CFLAGS "-O0 -g -I${VCS_HOME}/include -I../../cpp/fringeVCS -I../../cpp/utils -I../dramShim -I../DRAMSim2 -I../ -fPIC -std=c++11 -L../ -ldramsim -lstdc++ -Wl,-rpath=../"

all-exp: bigIP dram compile elaborate

all: dram sim

elaborate:
	vcs -fsdb -cpp ${CC} ${CC_OPTS} -full64 -debug_pp -t ps -licqueue xil_defaultlib.test sim.cpp -o accel.bit.bin

compile:
	vlogan ${VCS_OPTS} -Xcheck_p1800_2009=char -work xil_defaultlib  +incdir+${XILINX_VIVADO}/data/xilinx_vip/include *.v *.sv 

sim:
	vcs ${VCS_OPTS} -cpp ${CC} ${CC_OPTS} -o accel.bit.bin xil_defaultlib *.v *.sv sim.cpp

dram:
	make -j8 -C DRAMSim2 libdramsim.so
	ln -sf DRAMSim2/libdramsim.so .
#	make -C dramShim
#	ln -sf dramShim/dram .

bigIP:
	cp ../bigIP.tcl .
	cp ../cpp/fringeVCS/sim/buildBigIP.tcl .
	cp ../cpp/fringeVCS/sim/getBigIPRTL.sh .
	CLOCK_FREQ_MHZ=${CLOCK_FREQ_MHZ} bash getBigIPRTL.sh
	bash bigIP_setup.sh
	bash bigIP_compile.sh

clean:
	rm -rf *.o *.csrc *.daidir ${TOP} simv ucli.key *.cmd *.in *.out *.vcd *.vpd Sim
