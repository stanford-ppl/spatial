TOP=Top
EXE=accel.bit.bin
CC=g++

# Synopsys DesignWare IP blocks
DW_HOME=/cad/synopsys/dc_shell/J-2014.09-SP3/dw

# Option for setting random seed: +ntb_random_seed=<number>
VCS_OPTS=-full64 -quiet -timescale=1ns/1ps -sverilog -debug_pp -Mdir=${TOP}.csrc +v2k +vcs+lic+wait +vcs+initreg+random +define+CLOCK_PERIOD=1 +lint=TFIPC-L +libext++.v -y ${DW_HOME}/sim_ver +incdir+${DW_HOME}/dw02/src_ver
CC_OPTS=-LDFLAGS "-L../ -ldramsim -lstdc++ -Wl,-rpath=../" -CFLAGS "-O0 -g -I${VCS_HOME}/include -I../../cpp/fringeVCS -I../dramShim -I../DRAMSim2 -I../ -fPIC -std=c++11 -L../ -ldramsim -lstdc++ -Wl,-rpath=../"

all: dram sim

sim:
	export LM_LICENSE_FILE=27000@cadlic0.stanford.edu
	vcs ${VCS_OPTS} -cpp ${CC} ${CC_OPTS} -o accel.bit.bin *.v *.sv sim.cpp

dram:
	make -j8 -C DRAMSim2 libdramsim.so
	ln -sf DRAMSim2/libdramsim.so .
#	make -C dramShim
#	ln -sf dramShim/dram .
clean:
	rm -rf *.o *.csrc *.daidir ${TOP} simv ucli.key *.cmd *.in *.out *.vcd *.vpd Sim
