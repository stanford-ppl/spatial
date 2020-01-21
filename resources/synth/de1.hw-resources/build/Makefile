PROJECT=DE1_SoC_Computer
IP=SpatialIP
SUBMODULE_DIR=Computer_System

all:
	set -e
	quartus_sh --restore ${PROJECT}.qar
	cp ${IP}.v ${SUBMODULE_DIR}/synthesis/submodules/
	quartus_map --read_settings_files=on --write_settings_files=off ${PROJECT} -c ${PROJECT}
	quartus_fit --read_settings_files=off --write_settings_files=off ${PROJECT} -c ${PROJECT}
	quartus_asm --read_settings_files=off --write_settings_files=off ${PROJECT} -c ${PROJECT}
	quartus_sta ${PROJECT} -c ${PROJECT}
	quartus_cpf -c --configuration_mode=FPP --option=rbf_option ${PROJECT}.sof sp.rbf
	
clean:
	rm -rf *.rbf *.rpt *.map.* *.fit.* *.report *.summary
