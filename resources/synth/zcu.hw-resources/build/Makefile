ifndef XILINXD_LICENSE_FILE
export XILINXD_LICENSE_FILE=7193@cadlic0.stanford.edu
$(info set $$XILINXD_LICENSE_FILE to [${XILINXD_LICENSE_FILE}])
endif


all: accel.bit.bin

pre-build-checks:
ifndef CLOCK_FREQ_MHZ
$(error CLOCK_FREQ_MHZ is not set!)
endif

accel.bit.bin: accel.bit parClockFreq.sh
	bash convert_bitstream accel.bit accel.bit.bin

accel.bit: bd
	time vivado -mode batch -source vivado.tcl -tclargs ${CLOCK_FREQ_MHZ} 2>&1 | tee vivado_synthesis.log

reports:
	time vivado -mode batch -source getReports.tcl -tclargs ${CLOCK_FREQ_MHZ} 2>&1 | tee vivado_reports.log

bd: pre-build-checks design_1.v
	sed -i 's/design_1_Top_0_0/Top/' design_1.v
	sed -i 's/(\*/\/\/ (\*/' design_1.v

design_1.v:
	time vivado -mode batch -source bdproject.tcl -tclargs ${CLOCK_FREQ_MHZ} 2>&1 | tee vivado_bdproject.log

parClockFreq.sh:
	echo "#!/bin/bash" 															> 	$@
	echo "export CLOCK_FREQ_MHZ=${CLOCK_FREQ_MHZ}" 	>> 	$@

clean:
	rm -rf parClockFreq.sh BOOT.* accel.bit accel.bit.bin bd_project project_1 *.jou *.log *.rpt *.bin design_1.v design_1_wrapper.v
