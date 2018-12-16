ZCU_V_DIR=verilog-zcu

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
	@echo "make           : ZCU SW + HW build"
	@echo "make hw        : Build Chisel for ZCU"
	@echo "make sw        : Build software for ZCU"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp scripts/zcu.mk cpp/Makefile
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${ZCU_V_DIR} accel.bit.bin parClockFreq.sh -C ../cpp Top -C ../zcu.sw-resources/utils set_perms setClocks.sh run.sh

hw:
	echo "$$(date +%s)" > start.log
	sed -i "s/EPRINTF(/fprintf(stderr,/g" zcu.sw-resources/FringeContextZCU.h # Not sure why eprintf randomly crashes zcu

	sbt "runMain top.Instantiator --verilog --testArgs zcu"
	if [ "${KEEP_HIERARCHY}" = "1" ]; then sed -i "s/^module/(* keep_hierarchy = \"yes\" *) module/g" ${ZCU_V_DIR}/Top.v; fi
	mv ${BIGIP_SCRIPT} ${ZCU_V_DIR}/
	cat zcu.hw-resources/SRAMVerilogAWS.v >> ${ZCU_V_DIR}/Top.v
	cp zcu.hw-resources/build/* ${ZCU_V_DIR}
	mv ${ZCU_V_DIR}/fsbl.elf._ ${ZCU_V_DIR}/fsbl.elf
	mv ${ZCU_V_DIR}/u-boot.elf._ ${ZCU_V_DIR}/u-boot.elf
	sed -i "s/^set TARGET .*/set TARGET ZCU102/g" ${ZCU_V_DIR}/settings.tcl
	sed -i "s/bash convert_bitstream accel.bit accel.bit.bin.*/bash convert_bitstream accel.bit accel.bit.bin ZCU102/g" ${ZCU_V_DIR}/Makefile
	make -C ${ZCU_V_DIR}
	echo "$$(date +%s)" > end.log

hw-clean:
	make -C ${ZCU_V_DIR} clean
	rm -rf ${ZCU_V_DIR}
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}
	rm -rf target

sw-clean:
	make -C cpp clean
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
