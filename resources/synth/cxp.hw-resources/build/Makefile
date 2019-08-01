ifndef XILINXD_LICENSE_FILE
export XILINXD_LICENSE_FILE=7193@cadlic0.stanford.edu
$(info set $$XILINXD_LICENSE_FILE to [${XILINXD_LICENSE_FILE}])
endif


# all: accel.bit.bin
all: accel.bit

accel.bit: setup
	time vivado -mode batch -source vivado.tcl 2>&1 | tee vivado_synthesis.log

setup: 
	time vivado -mode batch -source create_vivado_project.tcl 2>&1 | tee vivado_setup.log 

clean:
	rm -rf parClockFreq.sh BOOT.* accel.bit accel.bit.bin bd_project project_1 *.jou *.log *.rpt *.bin design_1.v design_1_wrapper.v
