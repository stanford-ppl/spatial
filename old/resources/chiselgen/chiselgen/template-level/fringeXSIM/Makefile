TOP=Top
EXE=accel.bit.bin

CC=g++

# Clean the section below, ripped from aws-fpga
XSIM_OPTS=-full64 -quiet -timescale=1ns/1ps -sverilog -debug_pp -Mdir=${TOP}.csrc +v2k +xsim+lic+wait +xsim+initreg+random +define+CLOCK_PERIOD=1 +lint=TFIPC-L
CC_OPTS=-LDFLAGS "-L../ -ldramsim -lstdc++ -Wl,-rpath=../" -CFLAGS "-O0 -g -I${XSIM_HOME}/include -I../../cpp/fringeVCS -I../dramShim -I../DRAMSim2 -I../ -fPIC -std=c++11 -L../ -ldramsim -lstdc++ -Wl,-rpath=../"


all: dram xse sim

xsc:
	xsc FringeContext.c

sim:
	xelab -svlog Top-harness.sv -s accel -debug typical -sv_lib dpi.so -d CLOCK_PERIOD=1
	# xelab Top -prj accel.prj -s accel -debug typical -sv_lib dpi.so -sv_root . -d CLOCK_PERIOD=1
	# g++ -I/opt/Xilinx/Vivado/2017.1/bin/../data/xsim/include -O3 -c -o xsi_loader.o xsi_loader.cpp
	# g++ -I/opt/Xilinx/Vivado/2017.1/bin/../data/xsim/include -I../cpp/fringeXSIM -I. -ldramsim -lstdc++ -std=c++11 -IDRAMSim2 -IdramShim -O3 -c -o Top.o sim.cpp
	# g++ -Wl,--no-as-needed -ldl -lrt -o accel.bit.bin Top.o xsi_loader.o
	# export LM_LICENSE_FILE=27000@cadlic0.stanford.edu
	# xsim ${XSIM_OPTS} -cpp ${CC} ${CC_OPTS} -o accel.bit.bin SRAMVerilogSim.v ${TOP}.v ${TOP}-harness.sv sim.cpp

dram:
	make -j8 -C DRAMSim2 libdramsim.so
	ln -sf DRAMSim2/libdramsim.so .
#	make -C dramShim
#	ln -sf dramShim/dram .
clean:
	rm -rf *.o *.csrc *.daidir ${TOP} simv ucli.key *.cmd *.in *.out *.vcd *.vpd Sim
