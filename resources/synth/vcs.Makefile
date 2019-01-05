APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: hw sw 
	tar -czf TopVCS.tar.gz -C verilog-vcs accel.bit.bin -C ../cpp Top

help:
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : VCS SW + HW build"
	@echo "make proto     : VCS SW + HW for huge apps."
	@echo "make hw-proto  : Build Chisel for VCS with proto chisel->firrtl compile"
	@echo "make hw        : Build Chisel for VCS"
	@echo "make sw        : Build software for VCS"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

proto: hw-proto sw
	tar -czf TopVCS.tar.gz -C verilog-vcs accel.bit.bin -C ../cpp Top

sw:
	cp scripts/vcs.mk cpp/Makefile
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -j8 -C cpp
	ln -sf cpp/Top .

hw:
	echo "$$(date +%s)" > start.log
	if [[ ! -z "${REGRESSION_ENV}" ]]; then sed -i "s/vcdon = .*;/vcdon = 0;/g" vcs.hw-resources/Top-harness.sv; fi 
	sbt "runMain top.Instantiator --verilog --testArgs vcs"
	cp -r vcs.hw-resources/* verilog-vcs
	touch in.txt
	make -C verilog-vcs
	ln -sf verilog-vcs verilog
	echo "$$(date +%s)" > end.log

hw-proto:
	echo "$$(date +%s)" > start.log
	if [[ ! -z "${REGRESSION_ENV}" ]]; then sed -i "s/vcdon = .*;/vcdon = 0;/g" vcs.hw-resources/Top-harness.sv; fi 
	rm -rf verilog-vcs~ && mkdir verilog-vcs
	sbt "runMain top.Instantiator --proto --testArgs vcs"
	mv Top.pb verilog-vcs/
	firrtl -i verilog-vcs/Top.pb -X verilog -o verilog-vcs/Top.v
	cp -r vcs.hw-resources/* verilog-vcs
	touch in.txt
	make -C verilog-vcs
	ln -sf verilog-vcs verilog
	echo "$$(date +%s)" > end.log

hw-clean:
	make -C verilog-vcs clean
	rm -rf verilog-vcs
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}
	rm -rf target

sw-clean: 
	make -C cpp clean
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
