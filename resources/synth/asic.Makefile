VERILATOR_SRC=cpp/verilator
AWS_V_DIR=verilog-aws
AWS_V_SIM_DIR=verilog-aws-sim
ZYNQ_V_DIR=verilog-zynq
ZCU_V_DIR=verilog-zcu
ARRIA10_V_DIR=verilog-arria10
GHRD_DIR=/opt/intelFPGA_pro/17.1/ghrd_pr/a10_soc_devkit_ghrd

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: hw sw
	tar -czf TopVCS.tar.gz -C verilog-asic accel.bit.bin -C ../cpp Top


help:
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : ASIC SW + HW build"
	@echo "make hw        : Build Chisel for ASIC"
	@echo "make sw        : Build software for ASIC"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp scripts/vcs.mk cpp/Makefile
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -j8 -C cpp
	ln -sf cpp/Top .

hw:
	sbt "runMain spatialIP.Instantiator --verilog --testArgs asic"
	cp -r asic.hw-resources/* verilog-asic
	cp -r asic.hw-resources/build/* verilog-asic
	touch in.txt
	make -C verilog-asic
	ln -sf verilog-asic verilog

hw-clean: 
	make -C verilog-asic clean
	rm -rf verilog-asic
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}
	rm -rf target

sw-clean: 
	make -C cpp clean
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
