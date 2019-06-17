KCU1500_V_DIR=verilog-kcu1500

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
	@echo "export KEEP_HIERARCHY=1 # add keep_hierarchy annotation to all verilog modules"
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : KCU1500 SW + HW build"
	@echo "make hw        : Build Chisel for KCU1500"
	@echo "make sw        : Build software for KCU1500"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp scripts/kcu1500.mk cpp/Makefile
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${KCU1500_V_DIR} accel.bit.bin parClockFreq.sh -C ../cpp Top -C ../kcu1500.sw-resources/utils set_perms setClocks.sh run.sh

hw:
	echo "$$(date +%s)" > start.log
	sed -i "s/EPRINTF(/fprintf(stderr,/g" kcu1500.sw-resources/FringeContextKCU1500.h # Not sure why eprintf randomly crashes kcu1500

	sbt "runMain top.Instantiator --verilog --testArgs kcu1500"
	mv ${BIGIP_SCRIPT} ${KCU1500_V_DIR}/
	cat kcu1500.hw-resources/SRAMVerilogAWS.v >> ${KCU1500_V_DIR}/Top.v
	if [ "${KEEP_HIERARCHY}" = "1" ]; then sed -i "s/^module/(* keep_hierarchy = \"yes\" *) module/g" ${KCU1500_V_DIR}/Top.v; fi
	cp kcu1500.hw-resources/build/* ${KCU1500_V_DIR}
	mv ${KCU1500_V_DIR}/fsbl.elf._ ${KCU1500_V_DIR}/fsbl.elf
	mv ${KCU1500_V_DIR}/u-boot.elf._ ${KCU1500_V_DIR}/u-boot.elf
	sed -i "s/^set TARGET .*/set TARGET KCU1500/g" ${KCU1500_V_DIR}/settings.tcl
	sed -i "s/bash convert_bitstream accel.bit accel.bit.bin.*/bash convert_bitstream accel.bit accel.bit.bin KCU1500/g" ${KCU1500_V_DIR}/Makefile
	make -C ${KCU1500_V_DIR}
	echo "$$(date +%s)" > end.log

hw-clean:
	make -C ${KCU1500_V_DIR} clean
	rm -rf ${KCU1500_V_DIR}
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}
	rm -rf target

sw-clean:
	make -C cpp clean
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
