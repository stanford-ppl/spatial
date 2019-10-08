APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: hw sw 
	tar -czf TopVCS.tar.gz -C verilog-vcs accel.bit.bin -C ../cpp Top

help:
	@echo "------- INFO -------"
	@echo "export FRINGELESS=1 # do not compile Fringe module into SpatialIP.v"
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make             : VCS SW + HW build"
	@echo "make hw          : Build Chisel for VCS"
	@echo "make sw          : Build software for VCS"
	@echo "make hw-clean    : Delete all generated hw files"
	@echo "make sw-clean    : Delete all generated sw files"
	@echo "make clean       : Delete all compiled code"
	@echo "make dse-model   : Run dse performance model (optionally takes ARGS=\"tune <param> <value> ... ni <param> <value> ...\" for noninteractive execution or dse tuning) "     
	@echo "make final-model : Run final performance model (optionally takes ARGS=\"ni <param> <value> ...\" for noninteractive execution) "     
	@echo "make sensitivity : Run sensitivity analysis for parameters based on model (see model/AppSensitivity.scala for center parameters)"
	@echo "------- END HELP -------"

sw:
	cp vcs.sw-resources/Makefile cpp/Makefile
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -j8 -C cpp
	ln -sf cpp/Top .

hw:
	echo "$$(date +%s)" > start.log
	if [[ ! -z "${REGRESSION_ENV}" ]]; then sed -i "s/vcdon = .*;/vcdon = 0;/g" vcs.hw-resources/Top-harness.sv; fi
ifeq ($(FRINGELESS),1)
	sbt "runMain top.Instantiator --verilog --testArgs fringeless";
	mv verilog-fringeless verilog-vcs
else
	sbt "runMain top.Instantiator --verilog --testArgs vcs"
endif
	cp -r vcs.hw-resources/* verilog-vcs
	touch in.txt
	make -C verilog-vcs
	ln -sf verilog-vcs verilog
	echo "$$(date +%s)" > end.log

hw-clean:
	make -C verilog-vcs clean
	rm -rf verilog-vcs
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}
	rm -rf target

sw-clean: 
	make -C cpp clean
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}

clean: hw-clean sw-clean

dse-model: 
	sbt "; project model; runMain model.AppRuntimeModel_dse ${ARGS}"

final-model: 
	sbt "; project model; runMain model.AppRuntimeModel_final ${ARGS}"

sensitivity: 
	sbt "; project model; runMain model.AppSensitivity"

assemble-model:
	mv model/model_final.scala model/model_final
	sbt "; project model; assembly"
	mv model/model_final model/model_final.scala


null: # Null target for regression testing purposes
