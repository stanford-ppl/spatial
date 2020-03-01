DE1_DIR=verilog-de1

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
PROJECT=DE1_SoC_Computer
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: hw sw

help:

	@echo "------- INFO -------"
	@echo "export KEEP_HIERARCHY=1 # add dont_touch annotation to all verilog modules"
	@echo "export USE_BRAM=1 # add ram_style = block annotation to all verilog modules"
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : DE1 SW + HW build"
	@echo "make hw        : Build Chisel for DE1"
	@echo "make sw        : Build software for DE1"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp de1.sw-resources/Makefile cpp/Makefile
	sed -i 's/ifndef ZYNQ/ifndef DE1/g' ./cpp/TopHost.cpp ./cpp/structs.hpp
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${DE1_DIR} SpatialIP.rbf -C ../cpp Top -C ../de1.sw-resources/utils set_perms run.sh

hw:
	echo "$$(date +%s)" > start.log
	sbt "runMain spatialIP.Instantiator --verilog --testArgs de1"
	cat de1.hw-resources/SRAMVerilogAWS.v >> ${DE1_DIR}/SpatialIP.v
	cp de1.hw-resources/build/* ${DE1_DIR}
	sed -i 's/SRFF/SRFF_sp/g' ${DE1_DIR}/SpatialIP.v
	make -C ${DE1_DIR} -j8
	echo "$$(date +%s)" > end.log

hw-clean:
	rm -rf ${DE1_DIR}
	rm -rf ./prog

sw-clean:
	cd ./cpp && make clean

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
