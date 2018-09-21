echo "Starting analysis and synthesis..."
quartus_map --read_settings_files=on --write_settings_files=off DE1_SoC_Computer -c DE1_SoC_Computer > synth.report
echo "Analysis and synthesis is successful. Starting place and route..."
quartus_fit --read_settings_files=off --write_settings_files=off DE1_SoC_Computer -c DE1_SoC_Computer > pandr.report
echo "Place and route is successful. Starting generating programming files"
quartus_asm --read_settings_files=off --write_settings_files=off DE1_SoC_Computer -c DE1_SoC_Computer > bitgen.report
echo "Programming files generation is successful. Starting TimeQuest Timing Analysis"
quartus_sta DE1_SoC_Computer -c DE1_SoC_Computer > timing.report
echo "Start generate compressed bitstream"
quartus_cpf -c --configuration_mode=FPP --option=rbf_option DE1_SoC_Computer.sof sp.rbf > cvp.report
echo "sp.rbf generated. Please copy it to the working directory on FPGA ARM"
