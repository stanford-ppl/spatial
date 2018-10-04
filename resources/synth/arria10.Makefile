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
	@echo "alterad license: $(ALTERAD_LICENSE_FILE)"
	@echo "quartus version: $(USING_THIS_QUARTUS)"
	sbt "runMain top.Instantiator --verilog --testArgs arria10"
	sed -i 's/SRFF/SRFF_sp/g' ${ARRIA10_V_DIR}/Top.v
	sed -i 's/SRAMVerilogSim/SRAMVerilogAWS/g' ${ARRIA10_V_DIR}/Top.v
	sed -i 's/module\ Top/module Top_DUT/g' ${ARRIA10_V_DIR}/Top.v
	mv ${ARRIA10_V_DIR}/Top.v ${ARRIA10_V_DIR}/Top_DUT.v
	cp -r arria10.hw-resources/build/* ${ARRIA10_V_DIR}
	# TODO: mannually create the link
	cd ${ARRIA10_V_DIR} && if [[ -L ghrd_10as066n2 ]]; then rm ghrd_10as066n2; fi && ln -s ${GHRD_DIR}/ghrd_10as066n2 ghrd_10as066n2 && if [[ -L pr_base_static.qdb ]]; then rm pr_base_static.qdb; fi && ln -s ${GHRD_DIR}/pr_base_static.qdb pr_base_static.qdb && if [[ -d output_files ]]; then rm -rf output_files; fi && mkdir output_files && ln -s ${GHRD_DIR}/output_files/pr_base.sof ./output_files/pr_base.sof && ln -s ${GHRD_DIR}/output_files/pr_base.static.msf ./output_files/pr_base.static.msf && ln -s ${GHRD_DIR}/output_files/pr_base.pr_region.pmsf ./output_files/pr_base.pr_region.pmsf
	cd ${ARRIA10_V_DIR} && bash fit.sh
	echo "$$(date +%s)" > end.log

sw:
	cp scripts/arria10.mk cpp/Makefile
	# cp cpp/cpptypes.hpp cpp/datastructures
	# cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	# cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	sed -i 's/ifndef ZYNQ/ifndef ARRIA10/g' ./cpp/TopHost.cpp
	sed -i 's/typedef __int128 int128_t;/\/\/typedef __int128 int128_t;/g' ./cpp/TopHost.cpp
	sed -i 's/typedef __int128 int128_t;/\/\/typedef __int128 int128_t;/g' ./cpp/structs.hpp
	make -C cpp -j8
	cp ${ARRIA10_V_DIR}/output_files/pr_region_alt.rbf ./ && cp ./cpp/Top ./
	tar -czf $(APPNAME).tar.gz pr_region_alt.rbf Top
	rm pr_region_alt.rbf Top

sw-clean:
	cd cpp && make clean

hw-clean:
	rm -rf target

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
