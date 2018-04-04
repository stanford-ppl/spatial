#!/bin/bash

vivado -mode batch -source buildBigIP.tcl -tclargs ${CLOCK_FREQ_MHZ}

rm -f bigIP.vivado.f

for m in $(grep -o "module_name .*" bigIP.tcl | cut -f2 -d' '); do
	echo "\${CL_ROOT}/design/$m.vhd" >> bigIP.vivado.f
	cp ./bigIPSimulation/bigIPSimulation.srcs/sources_1/ip/$m/sim/$m.vhd .
done
