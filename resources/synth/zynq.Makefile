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
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : Zynq SW + HW build"
	@echo "make hw        : Build Chisel for Zynq"
	@echo "make sw        : Build software for Zynq"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp scripts/zynq.mk cpp/Makefile
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${ZYNQ_V_DIR} accel.bit.bin parClockFreq.sh -C ../cpp Top -C zynq.sw-resources/utils set_perms setClocks.sh run.sh

hw:
	echo "$$(date +%s)" > start.log
	sbt "runMain top.Instantiator --verilog --testArgs zynq"
	mv ${BIGIP_SCRIPT} ${ZYNQ_V_DIR}/
	cat zynq.hw-resources/SRAMVerilogAWS.v >> ${ZYNQ_V_DIR}/Top.v
	cp zynq.hw-resources/build/* ${ZYNQ_V_DIR}
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
