VERILATOR_SRC=cpp/verilator
AWS_V_DIR=verilog-aws
AWS_V_SIM_DIR=verilog-aws-sim
ZYNQ_V_DIR=verilog-zynq
ZCU_V_DIR=verilog-zcu
ARRIA10_V_DIR=verilog-arria10
GHRD_DIR=/opt/intelFPGA_pro/17.1/ghrd_pr/a10_soc_devkit_ghrd

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: help

help:
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make sim         : Verilator SW + HW build"
	@echo "make sim-sw      : Verilator SW build"
	@echo "make sim-hw      : Build Chisel for Verilator"
	@echo "make aws-sim     : AWS simulation SW + HW build"
	@echo "make aws-sim-hw  : Build Chisel for AWS simulation"
	@echo "make aws-F1      : AWS F1 SW + HW build"
	@echo "make aws-F1-hw   : Build Chisel for AWS F1"
	@echo "make aws-F1-afi  : Build Bitstream for AWS F1"
	@echo "make aws-F1-sw   : Build Host for AWS F1"
	@echo "make zynq        : Zynq SW + HW build"
	@echo "make zynq-hw     : Build Chisel for Zynq"
	@echo "make zynq-hw     : Build software for Zynq"
	@echo "make zcu        	: Zynq Ultrascale SW + HW build"
	@echo "make zcu-hw      : Build Chisel for Zynq Ultrascale"
	@echo "make zcu-hw      : Build software for Zynq Ultrascale"
	@echo "make vcs         : VCS SW + HW build"
	@echo "make asic        : ASIC SW + HW build"
	@echo "make asic-hw     : ASIC HW build"
	@echo "make asic-sw     : ASIC SW build"
	@echo "make vcs-hw      : Build Chisel for VCS"
	@echo "make xsim        : XSIM SW + HW build"
	@echo "make xsim-hw     : Build Chisel for XSIM"
	@echo "make sim-clean   : Verilator simulation clean up"
	@echo "make de1soc			: Build Chisel for DE1-SoC"
	@echo "make arria10			: Build Chisel for Arria10"
	@echo "------- END HELP -------"

sim: sim-hw
	make -C ${VERILATOR_SRC}
	ln -sf ${VERILATOR_SRC}/Top .

sim-sw:
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -C ${VERILATOR_SRC}
	ln -sf ${VERILATOR_SRC}/Top .

sim-hw:
	rm -rf ${VERILATOR_SRC}
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	cp chisel/template-level/fringeVCS/SRAMVerilogSim.v . || :
	sbt "runMain top.Instantiator --backend-name verilator --target-dir ${VERILATOR_SRC} --testArgs"
	cp scripts/verilator.mk ${VERILATOR_SRC}/Makefile
	mv ${VERILATOR_SRC}/top* ${VERILATOR_SRC}/verilator_srcs_tmp
	mkdir ${VERILATOR_SRC}/verilator_srcs
	mv ${VERILATOR_SRC}/verilator_srcs_tmp/VTop* ${VERILATOR_SRC}/verilator_srcs
	mv ${VERILATOR_SRC}/verilator_srcs_tmp/*.v ${VERILATOR_SRC}/verilator_srcs
	rm -rf ${VERILATOR_SRC}/verilator_srcs_tmp
	mv ${VERILATOR_SRC}/verilator_srcs/*.mk ${VERILATOR_SRC}

# ------------------------------------------------------------------------------
# START OF AWS TARGETS
# ------------------------------------------------------------------------------

# TODO: Use the following structure instead:
# aws-hw:
# aws-sim: aws-hw  aws-sim-tb
# aws-sim-tb:
# aws-F1 : aws-F1-sw
# aws-F1-dcp: aws-hw
# aws-F1-afi: aws-F1-dcp
# aws-F1-sw : aws-F1-afi

# Run simulation using Vivado XSIM
aws-sim: aws-sim-hw
	$(eval app_name=$(notdir $(shell pwd)))
	# ----------------------------------------------------------------------------
	# Compile the testbench and make the binary to call xsim run
	# NOTE: Requires hdk_setup.sh to have been already sourced
	# ----------------------------------------------------------------------------
	# Build the DPI .so library
	cd $(AWS_HOME)/hdk/cl/examples/${app_name}/verif/scripts && make C_TEST=test_spatial_main make_sim_dir compile
	# Create the binary
	sed 's:{{{INSERT_DESIGN_DIR}}}:'"${AWS_HOME}"'/hdk/cl/examples/${app_name}/verif/scripts:g' cpp/fringeAWS/Top_template > ./Top
	chmod 700 Top

# Set up simulation directory
# TODO: Refactor this into aws-hw (step 1) and aws-sim-tb (step 2), since aws-F1-hw reuses the aws-hw portion (step 1)
aws-sim-hw:
	$(eval app_name=$(notdir $(shell pwd)))
	# ----------------------------------------------------------------------------
	# Step 1: Make the design
	# ----------------------------------------------------------------------------
	rm -rf ${AWS_V_SIM_DIR}
	# First use chisel to create the verilog
	sbt "runMain top.Instantiator --verilog --testArgs aws-sim"
	cat chisel/template-level/fringeVCS/SRAMVerilogSim.v >> ${AWS_V_SIM_DIR}/Top.v
	cat chisel/template-level/fringeVCS/RetimeShiftRegister.sv >> ${AWS_V_SIM_DIR}/Top.v
	# Make a copy of the template directory
	rm -rf $(AWS_HOME)/hdk/cl/examples/${app_name}
	cp -r $(AWS_HOME)/hdk/cl/examples/cl_dram_dma $(AWS_HOME)/hdk/cl/examples/${app_name}
	# Add all the static design files
	cp -f cpp/fringeAWS/design/* $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	cp -f ${AWS_V_SIM_DIR}/Top.v $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	# Run a script to put design together
	python cpp/fringeAWS/gen_aws_design.py $(AWS_HOME)/hdk/cl/examples/${app_name}
	# ----------------------------------------------------------------------------
	# Step 2: Make the testbench
	# ----------------------------------------------------------------------------
	# Add all the static software files
	cp -f cpp/TopHost.cpp $(AWS_HOME)/hdk/cl/examples/${app_name}/software/src/
	cp -f cpp/*.h $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -f cpp/fringeAWS/headers/* $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -rf cpp/datastructures $(AWS_HOME)/hdk/cl/examples/${app_name}/software/src/
	# Add all the simulation Makefiles
	cp -f cpp/fringeAWS/sim/Makefile* $(AWS_HOME)/hdk/cl/examples/${app_name}/verif/scripts/
	cp -f cpp/fringeAWS/sim/test_null.sv $(AWS_HOME)/hdk/cl/examples/${app_name}/verif/tests/
	# Run a script to put tb together
	python cpp/fringeAWS/gen_aws_tb.py $(AWS_HOME)/hdk/cl/examples/${app_name}

aws-F1 : aws-F1-afi    aws-F1-sw

# Build the hardware
# This is very similar to step 1 of aws-sim-hw, can refactor to merge the two
aws-F1-hw:
	$(eval app_name=$(notdir $(shell pwd)))
	# ----------------------------------------------------------------------------
	# Make the design
	# ----------------------------------------------------------------------------
	rm -rf ${AWS_V_DIR}
	# First use chisel to create the verilog
	sbt "runMain top.Instantiator --verilog --testArgs aws"
	cat chisel/template-level/fringeAWS/SRAMVerilogAWS.v >> ${AWS_V_DIR}/Top.v
	cat chisel/template-level/fringeAWS/RetimeShiftRegister.sv >> ${AWS_V_DIR}/Top.v
	mv ${BIGIP_SCRIPT} ${AWS_V_DIR}/
	# Make a copy of the template directory
	rm -rf $(AWS_HOME)/hdk/cl/examples/${app_name}
	cp -r $(AWS_HOME)/hdk/cl/examples/cl_dram_dma $(AWS_HOME)/hdk/cl/examples/${app_name}
	# Add all the static design files
	cp -f cpp/fringeAWS/design/* $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	cp -f ${AWS_V_DIR}/Top.v $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	# Run a script to put design together
	python cpp/fringeAWS/gen_aws_design.py $(AWS_HOME)/hdk/cl/examples/${app_name}

# Build the bitstream
aws-F1-afi:   aws-F1-hw
	echo "$$(date +%s)" > start.log
	$(eval app_name=$(notdir $(shell pwd)))
	# ----------------------------------------------------------------------------
	# Step 1: Run synthesis
	# NOTE: Requires hdk_setup.sh to have been already sourced, and Vivado license
	# ----------------------------------------------------------------------------
	cp -f cpp/fringeAWS/build/encrypt.tcl $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts/
	cp -f cpp/fringeAWS/build/clockFreq.tcl $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts/
	cp -f cpp/fringeAWS/build/synth_cl_dram_dma.tcl $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts/
	cp -f ${AWS_V_DIR}/${BIGIP_SCRIPT} $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts/
	# NOTE: The DEFAULT strategy will be used, see others here:
	#       https://github.com/aws/aws-fpga/blob/master/hdk/common/shell_v04151701/new_cl_template/build/README.md
	# NOTE: This may fail (e.g. our of area) -- may need to rerun this manually
	ln -s ${AWS_HOME}/hdk/cl/examples/${app_name}/ aws_dir
	# NOTE: Clock recipes can be found here: https://github.com/aws/aws-fpga/blob/b4b9d74415a70f0470b02635604a34710cc1bb22/hdk/docs/clock_recipes.csv
ifeq ($(CLOCK_FREQ_MHZ),125)
	cd $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts && CL_DIR=$(AWS_HOME)/hdk/cl/examples/${app_name} bash aws_build_dcp_from_cl.sh -clock_recipe_a A0 -uram_option 4 ${FOREGROUND}
else ifeq ($(CLOCK_FREQ_MHZ),250)
	cd $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts && CL_DIR=$(AWS_HOME)/hdk/cl/examples/${app_name} bash aws_build_dcp_from_cl.sh -clock_recipe_a A1 -uram_option 4 ${FOREGROUND}
else ifeq ($(CLOCK_FREQ_MHZ),5.625)
	cd $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts && CL_DIR=$(AWS_HOME)/hdk/cl/examples/${app_name} bash aws_build_dcp_from_cl.sh -clock_recipe_a A2 -uram_option 4 ${FOREGROUND}
else
	$(error Invalid CLOCK_FREQ_MHZ ${CLOCK_FREQ_MHZ})
endif
	# Use the following line instead for faster builds:
	# cd $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts && CL_DIR=$(AWS_HOME)/hdk/cl/examples/${app_name} bash aws_build_dcp_from_cl.sh -strategy BASIC -clock_recipe_a A2
	# ----------------------------------------------------------------------------
	# Step 2: Upload bitstream to S3 and create AFI (currently done manually, TODO: script this)
	# This will eventually be a new Makefile target, which runs only when the final DCP is created (after synthesis/place/route)
	# ----------------------------------------------------------------------------
	echo "#!/bin/bash"												                                                 > create_spatial_AFI_instructions.sh
	echo "# Instructions to upload bitstream to S3 and create AFI"                                                  >> create_spatial_AFI_instructions.sh
	echo "cd $(AWS_HOME)/hdk/cl/examples/${app_name}"                                                               >> create_spatial_AFI_instructions.sh
	echo "aws s3 mb s3://${app_name}_$(timestamp)_bucket  --region us-east-1"                                       >> create_spatial_AFI_instructions.sh
	echo "aws s3 mb s3://${app_name}_$(timestamp)_bucket/dcp 2>&1 | tee log"                                             >> create_spatial_AFI_instructions.sh
	echo "too_many_buckets=\`cat log | grep TooManyBuckets | wc -l\`"							                        >> create_spatial_AFI_instructions.sh                 								
	echo "if [[ \$${too_many_buckets} -ne 0 ]]; then echo \"Too many buckets error.  Delete some in S3\"; exit 1; fi" >> create_spatial_AFI_instructions.sh 
	echo "aws s3 cp build/checkpoints/to_aws/*.Developer_CL.tar s3://${app_name}_$(timestamp)_bucket/dcp/ | tee log" >> create_spatial_AFI_instructions.sh
	echo "tarname=\`cat log | grep \"Developer_CL.tar\" -m 1 | rev | cut -d/ -f1 | rev\`"                             >> create_spatial_AFI_instructions.sh
	echo "if [[ -z \$$tarname ]]; then echo \"No tarname found!\"; exit 1; fi"                                          >> create_spatial_AFI_instructions.sh
	echo "aws s3 mb s3://${app_name}_$(timestamp)_bucket/logs | tee log"                                            >> create_spatial_AFI_instructions.sh
	echo "touch LOGS_FILES_GO_HERE.txt"                                                                             >> create_spatial_AFI_instructions.sh
	echo "aws s3 cp LOGS_FILES_GO_HERE.txt s3://${app_name}_$(timestamp)_bucket/logs/ 2>&1 | tee log"                    >> create_spatial_AFI_instructions.sh
	echo ""                                                                                                         >> create_spatial_AFI_instructions.sh
	echo "# Create the FPGA Image."                                                                                 >> create_spatial_AFI_instructions.sh
	echo "# If this command fails, you may need a different awscli version. We tested with version 1.11.78."        >> create_spatial_AFI_instructions.sh
	echo "# Important: Replace <tarball-name> below with the name of the tarball file copied to S3 above,"          >> create_spatial_AFI_instructions.sh
	echo "#            e.g. replace <tarball-name> with 17_10_06-######.Developer_CL.tar."                          >> create_spatial_AFI_instructions.sh
	echo "aws ec2 create-fpga-image \\"                                                                             >> create_spatial_AFI_instructions.sh
	echo "--name ${app_name} \\"                                                                                    >> create_spatial_AFI_instructions.sh
	echo "--input-storage-location Bucket=${app_name}_$(timestamp)_bucket,Key=dcp/\$$tarname \\"                     >> create_spatial_AFI_instructions.sh
	echo "--logs-storage-location Bucket=${app_name}_$(timestamp)_bucket,Key=logs/ | tee fpgaIds"                   >> create_spatial_AFI_instructions.sh
	echo ""                                                                                                         >> create_spatial_AFI_instructions.sh
	echo "# Keep a record of the afi and agfi IDs returned above."                                                  >> create_spatial_AFI_instructions.sh
	echo "# Now wait for the logs to be created in S3. The State file should indicate that the AFI is available"    >> create_spatial_AFI_instructions.sh
	echo "# Once that is done, see the online Spatial AWS documentation for how to open and run on an F1 instance." >> create_spatial_AFI_instructions.sh
	echo "echo \"FpgaImageId and FpgaImageGlobalId stored in \`pwd\`/fpgaIds\" "                                      >> create_spatial_AFI_instructions.sh
	echo ""                                                                                                         >> create_spatial_AFI_instructions.sh
	#
	# *** Place and Route is now running in the background.                             ***
	# *** When it completes, follow instructions in create_spatial_AFI_instructions.sh ***
	#
	echo "$$(date +%s)" >> end.log

aws-F1-sw:
	$(eval app_name=$(notdir $(shell pwd)))
	# ----------------------------------------------------------------------------
	# Make the host binary
	# NOTE: Requires sdk_setup.sh to have been sourced, and aws-F1-hw to have been run
	# ----------------------------------------------------------------------------
	# Add all the static software files
	cp -f cpp/TopHost.cpp $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime/
	cp -f cpp/*.h $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -f cpp/*.hpp $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -f cpp/fringeAWS/headers/* $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -rf cpp/datastructures $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime/
	# Compile
	cp -f cpp/fringeAWS/build/Makefile $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime/
	cp -f cpp/fringeAWS/build/load.sh $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime/
	cd $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime && make all
	# The 'Top' binary is now in the software/runtime directory mentioned in the previous line
	# Important: run this on the F1 using sudo, e.g. 'sudo ./Top arg1 arg2'
	# If this compilation was not done on the F1, you may need to change permissions on the F1, e.g. using 'chmod 700 Top'

# ------------------------------------------------------------------------------
# END OF AWS TARGETS
# ------------------------------------------------------------------------------


zynq: zynq-hw zynq-sw

zynq-sw:
	cp scripts/zynq.mk cpp/Makefile
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${ZYNQ_V_DIR} accel.bit.bin parClockFreq.sh -C ../cpp Top -C fringeZynq/utils set_perms setClocks.sh run.sh

zynq-hw:
	# sed -i "s/val bug239_hack = .*/val bug239_hack = true/g" chisel/template-level/fringeHW/Fringe.scala
	echo "$$(date +%s)" > start.log
	sbt "runMain top.Instantiator --verilog --testArgs zynq"
	mv ${BIGIP_SCRIPT} ${ZYNQ_V_DIR}/
	cat chisel/template-level/fringeAWS/SRAMVerilogAWS.v >> ${ZYNQ_V_DIR}/Top.v
	cp chisel/template-level/fringeZynq/build/* ${ZYNQ_V_DIR}
	mv ${ZYNQ_V_DIR}/fsbl.elf._ ${ZYNQ_V_DIR}/fsbl.elf
	mv ${ZYNQ_V_DIR}/u-boot.elf._ ${ZYNQ_V_DIR}/u-boot.elf
	make -C ${ZYNQ_V_DIR}
	echo "$$(date +%s)" > end.log

zynq-clean:
	make -C ${ZYNQ_V_DIR} clean
	make -C cpp clean
	rm -rf ${ZYNQ_V_DIR}
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}
	rm -rf target

# Build the hardware for arria10
arria10: arria10-hw arria10-sw

arria10-hw:
	echo "$$(date +%s)" > start.log
	@echo "alterad license: $(ALTERAD_LICENSE_FILE)"
	@echo "quartus version: $(USING_THIS_QUARTUS)"
	sbt "runMain top.Instantiator --verilog --testArgs arria10"
	sed -i 's/SRFF/SRFF_sp/g' ${ARRIA10_V_DIR}/Top.v
	sed -i 's/SRAMVerilogSim/SRAMVerilogAWS/g' ${ARRIA10_V_DIR}/Top.v
	sed -i 's/module\ Top/module Top_DUT/g' ${ARRIA10_V_DIR}/Top.v
	mv ${ARRIA10_V_DIR}/Top.v ${ARRIA10_V_DIR}/Top_DUT.v
	cp -r chisel/template-level/fringeArria10/build/* ${ARRIA10_V_DIR}
	# TODO: mannually create the link
	cd ${ARRIA10_V_DIR} && if [[ -L ghrd_10as066n2 ]]; then rm ghrd_10as066n2; fi && ln -s ${GHRD_DIR}/ghrd_10as066n2 ghrd_10as066n2 && if [[ -L pr_base_static.qdb ]]; then rm pr_base_static.qdb; fi && ln -s ${GHRD_DIR}/pr_base_static.qdb pr_base_static.qdb && if [[ -d output_files ]]; then rm -rf output_files; fi && mkdir output_files && ln -s ${GHRD_DIR}/output_files/pr_base.sof ./output_files/pr_base.sof && ln -s ${GHRD_DIR}/output_files/pr_base.static.msf ./output_files/pr_base.static.msf && ln -s ${GHRD_DIR}/output_files/pr_base.pr_region.pmsf ./output_files/pr_base.pr_region.pmsf
	cd ${ARRIA10_V_DIR} && bash fit.sh
	echo "$$(date +%s)" > end.log

arria10-sw:
	cp scripts/arria10.mk cpp/Makefile
	# cp cpp/cpptypes.hpp cpp/datastructures
	# cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	# cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	sed -i 's/ifndef ZYNQ/ifndef ARRIA10/g' ./cpp/TopHost.cpp
	make -C cpp -j8
	cp ${ARRIA10_V_DIR}/output_files/pr_region_alt.rbf ./ && cp ./cpp/Top ./
	tar -czf $(APPNAME).tar.gz pr_region_alt.rbf Top
	rm pr_region_alt.rbf Top

arria10-sw-clean:
	cd cpp && make clean


zcu: zcu-hw zcu-sw

zcu-sw:
	cp scripts/zcu.mk cpp/Makefile
	make -C cpp -j8
	tar -czf $(APPNAME).tar.gz -C ${ZCU_V_DIR} accel.bit.bin parClockFreq.sh -C ../cpp Top -C fringeZynq/utils set_perms setClocks.sh run.sh

zcu-hw:
	echo "$$(date +%s)" > start.log
	# Some hacks that really need to be fixed in a way better than sed
	# sed -i "s/val bug239_hack = .*/val bug239_hack = true/g" chisel/template-level/fringeHW/Fringe.scala
	# sed -i "s/#define FRINGE_MEM_BASEADDR.*/#define FRINGE_MEM_BASEADDR  0x40000000/g" cpp/fringeZynq/ZynqAddressMap.h
	# sed -i "s/#define FRINGE_SCALAR_BASEADDR.*/#define FRINGE_SCALAR_BASEADDR  0x400000000/g" cpp/fringeZynq/ZynqAddressMap.h
	# sed -i "s/#define MEM_SIZE.*/#define MEM_SIZE  0x40000000/g" cpp/fringeZynq/ZynqAddressMap.h
	sed -i "s/EPRINTF(/fprintf(stderr,/g" cpp/fringeZynq/FringeContextZynq.h # Not sure why eprintf randomly crashes zcu

	sbt "runMain top.Instantiator --verilog --testArgs zcu"
	mv ${BIGIP_SCRIPT} ${ZCU_V_DIR}/
	cat chisel/template-level/fringeAWS/SRAMVerilogAWS.v >> ${ZCU_V_DIR}/Top.v
	cp chisel/template-level/fringeZynq/build/* ${ZCU_V_DIR}
	mv ${ZCU_V_DIR}/fsbl.elf._ ${ZCU_V_DIR}/fsbl.elf
	mv ${ZCU_V_DIR}/u-boot.elf._ ${ZCU_V_DIR}/u-boot.elf
	sed -i "s/^set TARGET .*/set TARGET ZCU102/g" ${ZCU_V_DIR}/settings.tcl
	sed -i "s/bash convert_bitstream accel.bit accel.bit.bin.*/bash convert_bitstream accel.bit accel.bit.bin ZCU102/g" ${ZCU_V_DIR}/Makefile
	make -C ${ZCU_V_DIR}
	echo "$$(date +%s)" > end.log

zcu-clean:
	make -C ${ZCU_V_DIR} clean
	make -C cpp clean
	rm -rf ${ZCU_V_DIR}
	rm -f $(APPNAME).tar.gz ${BIGIP_SCRIPT}
	rm -rf target

de1soc: de1soc-hw de1soc-sw

de1soc-sw:
	cp scripts/de1soc.mk cpp/Makefile
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

de1soc-hw:
	sbt "runMain top.Instantiator --verilog --testArgs de1soc"
	sed -i 's/SRFF/SRFF_sp/g' verilog-de1soc/Top.v
	cp -r chisel/template-level/fringeDE1SoC/simulation verilog-de1soc/
	cp -r chisel/template-level/fringeDE1SoC/* verilog-de1soc/
	cp verilog-de1soc/Top.v verilog-de1soc/Computer_System/synthesis/submodules
	cd verilog-de1soc && chmod +x compile.sh && ./compile.sh
	echo "sp.rbf generated. Please copy it to your working dir on FPGA ARM"
	cp verilog-de1soc/sp.rbf ./

de1soc-clean:
	rm -rf verilog-de1soc
	rm -rf ./prog
	cd ./cpp && make clean

vcs: vcs-hw vcs-sw
	tar -czf TopVCS.tar.gz -C verilog-vcs accel.bit.bin -C ../cpp Top

vcs-sw:
	cp scripts/vcs.mk cpp/Makefile
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -j8 -C cpp
	ln -sf cpp/Top .

vcs-hw:
	echo "$$(date +%s)" > start.log
	if [[ ! -z "${REGRESSION_ENV}" ]]; then sed -i "s/vcdon = .*;/vcdon = 0;/g" chisel/template-level/fringeVCS/Top-harness.sv; fi 
	# sed -i "s/val bug239_hack = .*/val bug239_hack = false/g" chisel/template-level/fringeHW/Fringe.scala
	sbt "runMain top.Instantiator --verilog --testArgs vcs"
	cp -r chisel/template-level/fringeVCS/* verilog-vcs
	touch in.txt
	make -C verilog-vcs
	ln -sf verilog-vcs verilog
	echo "$$(date +%s)" > end.log

vcs-clean:
	make -C verilog-vcs clean
	make -C cpp clean
	rm -rf verilog-vcs
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}
	rm -rf target

asic: asic-hw asic-sw
	tar -czf TopVCS.tar.gz -C verilog-asic accel.bit.bin -C ../cpp Top

asic-sw: vcs-sw

asic-hw:
	# sed -i "s/val bug239_hack = .*/val bug239_hack = false/g" chisel/template-level/fringeHW/Fringe.scala
	sbt "runMain top.Instantiator --verilog --testArgs asic"
	cp -r chisel/template-level/fringeVCS/* verilog-asic
	cp -r chisel/template-level/fringeASIC/build/* verilog-asic
	touch in.txt
	make -C verilog-asic
	ln -sf verilog-asic verilog

asic-clean:
	make -C verilog-asic clean
	make -C cpp clean
	rm -rf verilog-asic
	rm -f verilog TopVCS.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}
	rm -rf target


xsim: xsim-hw xsim-sw
	tar -czf TopXSIM.tar.gz -C verilog-xsim accel.bit.bin -C ../cpp Top

xsim-sw:
	cp scripts/xsim.mk cpp/Makefile
	cp cpp/cpptypes.hpp cpp/datastructures
	cp cpp/Structs.h cpp/datastructures 2>/dev/null || :
	cp cpp/cppDeliteArrayStructs.h cpp/datastructures 2>/dev/null || :
	make -j8 -C cpp
	ln -sf cpp/Top .

xsim-hw:
	sbt "runMain top.Instantiator --verilog --testArgs xsim"
	cp -r chisel/template-level/fringeXSIM/* verilog-xsim
	touch in.txt
	make -C verilog-xsim
	ln -sf verilog-xsim verilog

xsim-clean:
	make -C verilog-xsim clean
	make -C cpp clean
	rm -rf verilog-xsim
	rm -f verilog TopXSIM.tar.gz Top *.log *.vcd ucli.key ${BIGIP_SCRIPT}
	rm -rf target

sim-clean:
	make -C ${VERILATOR_SRC} clean
	rm -f Top *.vcd

null: # Null target for regression testing purposes
