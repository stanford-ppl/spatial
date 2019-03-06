ARRIA10_V_DIR=verilog-arria10

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: hw sw

help:
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : Arria10 SW + HW build"
	@echo "make hw        : Build Chisel for Arria10"
	@echo "make sw        : Build software for Arria10"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

hw:
	echo "$$(date +%s)" > start.log
	@echo "quartus version: $(QVER)"
	sbt "runMain top.Instantiator --verilog --testArgs arria10"
	sed -i 's/SRFF/SRFF_sp/g' ${ARRIA10_V_DIR}/Top.v
	sed -i 's/SRAMVerilogSim/SRAMVerilogAWS/g' ${ARRIA10_V_DIR}/Top.v
	tar -xf arria10.hw-resources/build/verilog-arria10.tar.gz -C ${ARRIA10_V_DIR}
	cp ${ARRIA10_V_DIR}/Top.v ${ARRIA10_V_DIR}/ip/pr_region_1_persona/pr_region_1_persona_Top_0/Top_10/synth/Top.v
	make -C ${ARRIA10_V_DIR}
	echo "$$(date +%s)" > end.log

sw:
	cp scripts/arria10.mk cpp/Makefile
	sed -i 's/ifndef ZYNQ/ifndef ARRIA10/g' ./cpp/TopHost.cpp
	sed -i 's/typedef __int128 int128_t;/\/\/typedef __int128 int128_t;/g' ./cpp/TopHost.cpp
	sed -i 's/typedef __int128 int128_t;/\/\/typedef __int128 int128_t;/g' ./cpp/structs.hpp
	make -C cpp -j8
	tar -czf ${APPNAME}.tar.gz -C ${ARRIA10_V_DIR} output_files/persona1.rbf -C ../cpp Top -C ../arria10.sw-resources/utils run.sh

sw-clean:
	cd cpp && make clean

hw-clean:
	rm -rf target

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
