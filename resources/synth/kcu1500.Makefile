KCU1500_V_DIR=verilog-kcu1500

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: hw

help: 
	@echo "------- INFO -------"
	@echo "export KEEP_HIERARCHY=1 # add keep_hierarchy annotation to all verilog modules"
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make   : KCU1500 SW + HW build"
	@echo "make hw: Build Chisel for KCU1500"
	@echo "make hw-clean  : Delete all generated hw files"
	@echo "make clean     : Delete all compiled code"
	@echo "------- END HELP -------"
hw:
	echo "$$(date +%s)" > start.log
	test -d cpp || mkdir cpp # to make fringe happy...
	sbt "runMain spatialIP.Instantiator --verilog --testArgs kcu1500"
	mv ${BIGIP_SCRIPT} ${KCU1500_V_DIR}/
	if [ "${KEEP_HIERARCHY}" = "1" ] && [ "${USE_BRAM}" = "1" ]; then sed -i "s/^module/(* DONT_TOUCH = \"yes\", RAM_STYLE = \"block\" *) module/g" ${CXP_V_DIR}/SpatialIP.v; \
	else if [ "${KEEP_HIERARCHY}" = "1" ]; then sed -i "s/^module/(* DONT_TOUCH = \"yes\" *) module/g" ${KCU1500_V_DIR}/SpatialIP.v; \
	else if [ "${USE_BRAM}" = "1" ]; then sed -i "s/^module/(* RAM_STYLE = \"block\" *) module/g" ${KCU1500_V_DIR}/SpatialIP.v; \
	fi; fi; fi;
	cat kcu1500.hw-resources/SRAMVerilogAWS.v >> ${KCU1500_V_DIR}/SpatialIP.v
	echo "$$(date +%s)" > end.log
	echo "1) Run the following:"
	echo "  cp ${KCU_1500_V_DIR}/SpatialIP.v $$TIMETOOL_HOME/firmware/targets/GenericSpatialApp/hdl/SpatialIP.v"
	echo "  cp python/TopHost.py $$TIMETOOL_HOME/software/scripts/TopHost.py"
	echo "  cp python/_AccelTop.py $$TIMETOOL_HOME/software/scripts/_AccelTop.py"
	echo "Then run the rest of the flow in the ROGUE framwork:"
	echo "  cd $$TIMETOOL_HOME/firmware/targets/GenericSpatialApp"
	echo "  make clean && make vcs"
	echo "Follow the steps that ROGUE gives you and then run `$$TIMETOOL_HOME/software/scripts/HostWrapper.py --dev sim` after you start the FPGA simulator with `simv`"
#ifeq (,$(wildcard $$TIMETOOL_HOME/firmware))
#	$(error timetool-spatial repo not found.  Please clone from https://github.com/slaclab/timetool-spatial and export TIMETOOL_HOME to point to it.)
#endif


hw-clean:
	make -C ${KCU1500_V_DIR} clean
	rm -rf ${KCU1500_V_DIR}
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}
	rm -rf target

clean: hw-cleans

null: # Null target for regression testing purposes

