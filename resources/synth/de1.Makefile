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
	@echo "make           : DE1SoC SW + HW build"
	@echo "make hw        : Build Chisel for DE1SoC"
	@echo "make sw        : Build software for DE1SoC"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp de1.sw-resources/Makefile cpp/Makefile
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/DE1SoC.h cpp/fringeDE1SoC/
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -C cpp
	ln -sf cpp/Top .
	cp verilog-de1soc/program_de1soc.sh ./ && chmod +x program_de1soc.sh
	rm -rf ./prog
	mkdir ./prog
	cd ./prog/ && mkdir verilog
	cp Top program_de1soc.sh ./prog
	cp sp.rbf ./prog/verilog/accel.bit.bin

hw:
	sbt "runMain spatialIP.Instantiator --verilog --testArgs de1"
# 	sed -i 's/SRFF/SRFF_sp/g' verilog-de1soc/SpatialIP.v
# 	cp -r hw-resources/simulation verilog-de1soc/
# 	cp -r hw-resources/* verilog-de1soc/
# 	cp verilog-de1soc/SpatialIP.v verilog-de1soc/Computer_System/synthesis/submodules
# 	cd verilog-de1soc && chmod +x compile.sh && ./compile.sh
# 	echo "sp.rbf generated. Please copy it to your working dir on FPGA ARM"
# 	cp verilog-de1soc/sp.rbf ./

hw-clean:
	rm -rf verilog-de1soc
	rm -rf ./prog

sw-clean:
	cd ./cpp && make clean

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
