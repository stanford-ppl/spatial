CXP_V_DIR=verilog-cxp

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: chisel hw sw 

.PHONY: all sw chisel hw

help:
	@echo "------- INFO -------"
	@echo "export KEEP_HIERARCHY=1 # add dont_touch annotation to all verilog modules"
	@echo "export USE_BRAM=1 # add ram_style = block annotation to all verilog modules"
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make           : CXP SW + HW build"
	@echo "make hw        : Build Chisel for CXP"
	@echo "make sw        : Build software for CXP"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make sw-clean  : Delete all generated sw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"

sw:
	cp scripts/cxp.mk cpp/Makefile
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${CXP_V_DIR} accel.bit.bin parClockFreq.sh -C ../cpp Top -C ../cxp.sw-resources/utils set_perms setClocks.sh run.sh

chisel: 
	echo "$$(date +%s)" > start.log
	sbt "runMain spatialIP.Instantiator --verilog --testArgs cxp"

hw:
ifeq (,$(wildcard cxp.hw-resources/euresys/CoaxlinkQuadCxp12_1cam/))
    $(error Euresys files not found!  Fix by running `git submodule update` in Spatial and recompile app.  Quick fix is to `cp -r $$SPATIAL_HOME/resources/synth/cxp.hw-resources/euresys ./cxp.hw-resources/` after updating submodules)
endif
	#@[ "${CXP_EXAMPLE}" ] || ( echo ">> CXP_EXAMPLE is not set"; exit 1 )
	mv ${BIGIP_SCRIPT} ${CXP_V_DIR}/
	cat cxp.hw-resources/SRAMVerilogAWS.v >> ${CXP_V_DIR}/SpatialIP.v
	if [ "${KEEP_HIERARCHY}" = "1" ] && [ "${USE_BRAM}" = "1" ]; then sed -i "s/^module/(* DONT_TOUCH = \"yes\", RAM_STYLE = \"block\" *) module/g" ${CXP_V_DIR}/SpatialIP.v; \
	else if [ "${KEEP_HIERARCHY}" = "1" ]; then sed -i "s/^module/(* DONT_TOUCH = \"yes\" *) module/g" ${CXP_V_DIR}/SpatialIP.v; \
	else if [ "${USE_BRAM}" = "1" ]; then sed -i "s/^module/(* RAM_STYLE = \"block\" *) module/g" ${CXP_V_DIR}/SpatialIP.v; \
	fi; fi; fi;
	cp cxp.hw-resources/build/* ${CXP_V_DIR}
	#cp -r ${CXP_EXAMPLE}/02_coaxlink ${CXP_V_DIR}
	cp -r cxp.hw-resources/euresys/CoaxlinkQuadCxp12_1cam/02_coaxlink ${CXP_V_DIR}
	cp *.v ${CXP_V_DIR} 2>/dev/null || : # hack for grabbing any blackboxes that may have been dumped here
	#cp ${CXP_EXAMPLE}/03_scripts/*.tcl ${CXP_V_DIR}
	cp cxp.hw-resources/euresys/CoaxlinkQuadCxp12_1cam/03_scripts/*.tcl ${CXP_V_DIR}
	sed -i "s/\.\.\///g" ${CXP_V_DIR}/create_vivado_project.tcl
	#cp -r ${CXP_EXAMPLE}/04_ref_design ${CXP_V_DIR}
	cp -r cxp.hw-resources/euresys/CoaxlinkQuadCxp12_1cam/04_ref_design ${CXP_V_DIR}
	# Replace custom logic with spatial rtl
	mv ${CXP_V_DIR}/SpatialCustomLogic.vhd ${CXP_V_DIR}/04_ref_design/CustomLogic.vhd
	make -C ${CXP_V_DIR}
	echo "$$(date +%s)" > end.log
	echo "Hooray! Bitstream has been generated at ${CXP_V_DIR}/accel.bit.  Go to ${CXP_EXAMPLE}/0 whatever and flash fpga with this bitstream "

hw-clean:
	make -C ${CXP_V_DIR} clean
	rm -rf ${CXP_V_DIR}
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}
	rm -rf target

sw-clean:
	make -C cpp clean
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}

clean: hw-clean sw-clean

null: # Null target for regression testing purposes
