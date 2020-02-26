ZYNQ_V_DIR=verilog-zynq

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
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
	@echo "make           : Zynq SW + HW build"
	@echo "make hw        : Build Chisel for Zynq"
	@echo "make sw        : Build software for Zynq"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp zynq.sw-resources/Makefile cpp/Makefile
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${ZYNQ_V_DIR} accel.bit.bin parClockFreq.sh -C ../cpp Top -C ../zynq.sw-resources/utils set_perms setClocks.sh run.sh

hw:
	echo "$$(date +%s)" > start.log
	sbt "runMain spatialIP.Instantiator --verilog --testArgs zynq"
	mv ${BIGIP_SCRIPT} ${ZYNQ_V_DIR}/

	## experimental stuff for v7
	#rm -rf zynq.hw-resources
	#cp -r ../resources/synth/virtex7.hw-resources/ zynq.hw-resources/

	cat zynq.hw-resources/SRAMVerilog*.v >> ${ZYNQ_V_DIR}/SpatialIP.v
	cp zynq.hw-resources/build/* ${ZYNQ_V_DIR}
	cp *.v ${ZYNQ_V_DIR} 2>/dev/null || : # hack for grabbing any blackboxes that may have been dumped here
	if [ "${KEEP_HIERARCHY}" = "1" ] && [ "${USE_BRAM}" = "1" ]; then sed -i "s/^module/(* DONT_TOUCH = \"yes\", RAM_STYLE = \"block\" *) module/g" ${ZYNQ_V_DIR}/SpatialIP.v; \
	else if [ "${KEEP_HIERARCHY}" = "1" ]; then sed -i "s/^module/(* DONT_TOUCH = \"yes\" *) module/g" ${ZYNQ_V_DIR}/SpatialIP.v; \
	else if [ "${USE_BRAM}" = "1" ]; then sed -i "s/^module/(* RAM_STYLE = \"block\" *) module/g" ${ZYNQ_V_DIR}/SpatialIP.v; \
	fi; fi; fi;
	mv ${ZYNQ_V_DIR}/fsbl.elf._ ${ZYNQ_V_DIR}/fsbl.elf
	mv ${ZYNQ_V_DIR}/u-boot.elf._ ${ZYNQ_V_DIR}/u-boot.elf
	make -C ${ZYNQ_V_DIR}
	echo "$$(date +%s)" > end.log

sw-clean: 
	make -C cpp clean
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}

hw-clean: 
	make -C ${ZYNQ_V_DIR} clean
	rm -rf ${ZYNQ_V_DIR}
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}
	rm -rf target

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
