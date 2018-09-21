VERILATOR_SRC=cpp/verilator

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: hw sw
	make -C ${VERILATOR_SRC}
	ln -sf ${VERILATOR_SRC}/Top .

help:
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : Scala SW + HW build"
	@echo "make hw        : Build Chisel for Scala"
	@echo "make sw        : Build software for Scala"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -C ${VERILATOR_SRC}
	ln -sf ${VERILATOR_SRC}/Top .

hw:
	rm -rf ${VERILATOR_SRC}
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	cp sim.hw-resources/SRAMVerilogSim.v . || :
	sbt "runMain top.Instantiator --backend-name verilator --target-dir ${VERILATOR_SRC} --testArgs"
	cp scripts/verilator.mk ${VERILATOR_SRC}/Makefile
	mv ${VERILATOR_SRC}/top* ${VERILATOR_SRC}/verilator_srcs_tmp
	mkdir ${VERILATOR_SRC}/verilator_srcs
	mv ${VERILATOR_SRC}/verilator_srcs_tmp/VTop* ${VERILATOR_SRC}/verilator_srcs
	mv ${VERILATOR_SRC}/verilator_srcs_tmp/*.v ${VERILATOR_SRC}/verilator_srcs
	rm -rf ${VERILATOR_SRC}/verilator_srcs_tmp
	mv ${VERILATOR_SRC}/verilator_srcs/*.mk ${VERILATOR_SRC}

hw-clean: 
	sbt clean

sw-clean: 
	sbt clean

clean: hw-clean sw-clean
