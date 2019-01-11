AWS_V_DIR=verilog-aws
AWS_V_SIM_DIR=verilog-aws-sim

APPNAME=$(shell basename $(shell pwd))
BIGIP_SCRIPT=bigIP.tcl
timestamp := $(shell /bin/date "+%Y-%m-%d---%H-%M-%S")
ifndef CLOCK_FREQ_MHZ
export CLOCK_FREQ_MHZ=125
$(info set $$CLOCK_FREQ_MHZ to [${CLOCK_FREQ_MHZ}])
endif

all: help

help:
	@echo "------- INFO -------"
	@echo "export KEEP_HIERARCHY=1 # add keep_hierarchy annotation to all verilog modules"
	@echo "------- SUPPORTED MAKE TARGETS -------"
	@echo "make aws-sim     : AWS simulation SW + HW build"
	@echo "make aws-sim-hw  : Build Chisel for AWS simulation"
	@echo "make aws-F1      : AWS F1 SW + HW build"
	@echo "make aws-F1-hw   : Build Chisel for AWS F1"
	@echo "make aws-F1-afi  : Build Bitstream for AWS F1"
	@echo "make aws-F1-sw   : Build Host for AWS F1"
	@echo "------- END HELP -------"

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
	# Link to build dir
	ln -s ${AWS_HOME}/hdk/cl/examples/${app_name}/ aws_dir
	# Create the binary
	sed 's:{{{INSERT_DESIGN_DIR}}}:'"${AWS_HOME}"'/hdk/cl/examples/${app_name}/verif/scripts:g' aws.sw-resources/Top_template > ./Top
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
	cat aws.hw-resources/SRAMVerilogSim.v >> ${AWS_V_SIM_DIR}/Top.v
	cat aws.hw-resources/RetimeShiftRegister.sv >> ${AWS_V_SIM_DIR}/Top.v
	if [ "${KEEP_HIERARCHY}" = "1" ]; then sed -i "s/^module/(* keep_hierarchy = \"yes\" *) module/g" ${AWS_V_DIR}/Top.v; fi
	# Make a copy of the template directory
	rm -rf $(AWS_HOME)/hdk/cl/examples/${app_name}
	cp -r $(AWS_HOME)/hdk/cl/examples/cl_dram_dma $(AWS_HOME)/hdk/cl/examples/${app_name}
	# Add all the static design files
	cp -f aws.sw-resources/design/* $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	cp -f ${AWS_V_SIM_DIR}/Top.v $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	# Run a script to put design together
	python aws.sw-resources/gen_aws_design.py $(AWS_HOME)/hdk/cl/examples/${app_name}
	# ----------------------------------------------------------------------------
	# Step 2: Make the testbench
	# ----------------------------------------------------------------------------
	# Add all the static software files
	cp -f cpp/TopHost.cpp $(AWS_HOME)/hdk/cl/examples/${app_name}/software/src/
	cp -f cpp/*.h $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -f cpp/*.hpp $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -f aws.sw-resources/headers/* $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -rf cpp/datastructures $(AWS_HOME)/hdk/cl/examples/${app_name}/software/src/
	# Add all the simulation Makefiles
	cp -f aws.sw-resources/sim/Makefile* $(AWS_HOME)/hdk/cl/examples/${app_name}/verif/scripts/
	cp -f aws.sw-resources/sim/test_null.sv $(AWS_HOME)/hdk/cl/examples/${app_name}/verif/tests/
	# Run a script to put tb together
	python aws.sw-resources/gen_aws_tb.py $(AWS_HOME)/hdk/cl/examples/${app_name}

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
	cat aws.hw-resources/SRAMVerilogAWS.v >> ${AWS_V_DIR}/Top.v
	cat aws.hw-resources/RetimeShiftRegister.sv >> ${AWS_V_DIR}/Top.v
	mv ${BIGIP_SCRIPT} ${AWS_V_DIR}/
	# Make a copy of the template directory
	rm -rf $(AWS_HOME)/hdk/cl/examples/${app_name}
	cp -r $(AWS_HOME)/hdk/cl/examples/cl_dram_dma $(AWS_HOME)/hdk/cl/examples/${app_name}
	# Add all the static design files
	cp -f aws.sw-resources/design/* $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	cp -f ${AWS_V_DIR}/Top.v $(AWS_HOME)/hdk/cl/examples/${app_name}/design/
	# Run a script to put design together
	python aws.sw-resources/gen_aws_design.py $(AWS_HOME)/hdk/cl/examples/${app_name}

# Build the bitstream
aws-F1-afi:   aws-F1-hw
	echo "$$(date +%s)" > start.log
	$(eval app_name=$(notdir $(shell pwd)))
	# ----------------------------------------------------------------------------
	# Step 1: Run synthesis
	# NOTE: Requires hdk_setup.sh to have been already sourced, and Vivado license
	# ----------------------------------------------------------------------------
	cp -f aws.sw-resources/build/encrypt.tcl $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts/
	cp -f aws.sw-resources/build/clockFreq.tcl $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts/
	cp -f aws.sw-resources/build/synth_cl_dram_dma.tcl $(AWS_HOME)/hdk/cl/examples/${app_name}/build/scripts/
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
	cp -f aws.sw-resources/headers/* $(AWS_HOME)/hdk/cl/examples/${app_name}/software/include/
	cp -rf cpp/datastructures $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime/
	# Compile
	cp -f aws.sw-resources/build/Makefile $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime/
	cp -f aws.sw-resources/build/load.sh $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime/
	cd $(AWS_HOME)/hdk/cl/examples/${app_name}/software/runtime && make all
	# The 'Top' binary is now in the software/runtime directory mentioned in the previous line
	# Important: run this on the F1 using sudo, e.g. 'sudo ./Top arg1 arg2'
	# If this compilation was not done on the F1, you may need to change permissions on the F1, e.g. using 'chmod 700 Top'

# ------------------------------------------------------------------------------
# END OF AWS TARGETS
# ------------------------------------------------------------------------------

null: # Null target for regression testing purposes
