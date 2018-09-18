################################################
#
# Makefile to Manage Quartus Prime Pro / QSys Design
#
# Copyright Intel (c) 2017
# All Rights Reserved
#
################################################

SHELL := /bin/bash

.SECONDEXPANSION:
.SUFFIXES:
.DELETE_ON_ERROR:

################################################
# Tools

CAT := cat
CD := cd
CHMOD := chmod
CP := cp -rf
DATE := date
ECHO := echo
FIND := find
GREP := grep
HEAD := head
MKDIR := mkdir -p
MV := mv
RM := rm -rf
SED := sed
TAR := tar
TOUCH := touch
WHICH := which

# Helpful Macros
SPACE := $(empty) $(empty)

ifndef COMSPEC
ifdef ComSpec
COMSPEC = $(ComSpec)
endif # ComSpec
endif # COMSPEC

ifdef COMSPEC # if Windows OS
IS_WINDOWS_HOST := 1
endif

ifeq ($(IS_WINDOWS_HOST),1)
ifneq ($(shell $(WHICH) cygwin1.dll 2>/dev/null),)
IS_CYGWIN_HOST := 1
endif
endif

ifneq ($(shell $(WHICH) quartus 2>/dev/null),)
HAVE_QUARTUS := 1
endif

ifeq ($(HAVE_QUARTUS),1)
# This makefile only supports Quartus Pro
HAVE_QUARTUS_PRO := 1
endif # HAVE_QUARTUS==1

ifeq ($(HAVE_QUARTUS),1)
HAVE_QSYS := 1
endif

ifeq ($(HAVE_QUARTUS_PRO),1)
# QSys Classic is no longer supported. QSys Pro must be used with Quartus Pro
HAVE_QSYS_PRO := 1
endif


################################################


################################################
#
# Design Settings
#  If you change and design settings, you need 
#  to run "make scrub_clean" followed by
#  "make generate_from_tcl" for the settings
#  to be applied
#

####
# Family and Device
# QUARTUS_DEVICE to Board Revision Mapping
#   Rev A Board: 10AS066N2F40I2SGES  [SoCEDS v15.1]
#   Rev B Board: 10AS066N3F40E2SGE2  [SoCEDS v15.1.1]
#   Rev C Board: 10AS066N3F40E2SG    [>= SoCEDS v16.0]

QUARTUS_DEVICE_FAMILY := Arria\ 10
QUARTUS_DEVICE := 10AS066N3F40E2SG
BOARD_REV := C

ifneq ($(QUARTUS_DEVICE_FAMILY),)
QUARTUS_TCL_ARGS += devicefamily $(QUARTUS_DEVICE_FAMILY)
QSYS_TCL_CMDS += set devicefamily $(QUARTUS_DEVICE_FAMILY);
endif

ifneq ($(QUARTUS_DEVICE),)
QUARTUS_TCL_ARGS += device $(QUARTUS_DEVICE)
QSYS_TCL_CMDS += set device $(QUARTUS_DEVICE);
endif

ifneq ($(BOARD_REV),)
QUARTUS_TCL_ARGS += board_rev $(BOARD_REV)
QSYS_TCL_CMDS += set board_rev $(BOARD_REV);
endif


####
ifeq ($(HAVE_QSYS_PRO),1)
QUARTUS_TCL_ARGS += qsys_pro $(HAVE_QSYS_PRO)
QSYS_TCL_CMDS += set qsys_pro $(HAVE_QSYS_PRO);
endif


####
# hps_sdram: input of memory device FBGA code to identify EMIF configuration for targeted memory. 
#   D9RPL = "MT41K512M16TNA-107" DDR3 Dual Ranks at 800MHz,
#   D9PZN = "MT41J256M16HA-093" DDR3 Single Rank at 1066MHz,
#   D9RGX = "MT40A256M16HA-093E:A" DDR4 Single Rank at 1066MHzx

HPS_SDRAM_SETTING := D9RGX
ifneq ($(HPS_SDRAM_SETTING),)
QUARTUS_TCL_ARGS += hps_sdram $(HPS_SDRAM_SETTING)
QSYS_TCL_CMDS += set hps_sdram $(HPS_SDRAM_SETTING);
endif


####
# Enable HPS_SDRAM ECC
ENABLE_HPS_SDRAM_ECC := 0
ifeq ($(ENABLE_HPS_SDRAM_ECC),1)
QUARTUS_TCL_ARGS += hps_sdram_ecc 1
QSYS_TCL_CMDS += set hps_sdram_ecc 1;
endif


####
# Enable Fast Trace (16bit trace through fpga core)
HPS_ENABLE_16BIT_TRACE := 0
ifeq ($(HPS_ENABLE_16BIT_TRACE),1)
QUARTUS_TCL_ARGS += fast_trace 1
QSYS_TCL_CMDS += set fast_trace 1;
endif


####
# Enable SGMII
HPS_ENABLE_SGMII := 0
HPS_SGMII_COUNT := 2
ifeq ($(HPS_ENABLE_SGMII),1)
QUARTUS_TCL_ARGS += hps_sgmii 1 sgmii_count $(HPS_SGMII_COUNT)
QSYS_TCL_CMDS += set hps_sgmii 1; set sgmii_count $(HPS_SGMII_COUNT);
endif


####
# hps_boot_device: select which pins the dedicated io will boot from
#   SDMMC = Boot from SD/MMC
#   QSPI  = Boot from QSPI
#   NAND  = Boot from NAND
HPS_BOOT_DEVICE := SDMMC
ifneq ($(HPS_BOOT_DEVICE),)
QUARTUS_TCL_ARGS += boot_device $(HPS_BOOT_DEVICE)
QSYS_TCL_CMDS += set boot_device $(HPS_BOOT_DEVICE);
endif

####
# Boot Selection override
# To override the Boot Select pins by the fgpa image,
# set the variable, BSEL, to one of the following integers:
# 	0 - RESERVED 
# 	1 - FPGA 
# 	2 - NAND Flash (1.8v) 
# 	3 - NAND Flash (3.0v) 
# 	4 - SD/MMC External Transceiver (1.8v) 
# 	5 - SD/MMC Internal Transceiver (3.0v) 
# 	6 - Quad SPI Flash (1.8v) 
# 	7 - Quad SPI Flash (3.0v)

ifneq ($(BSEL),)
QSYS_TCL_CMDS += set bsel $(BSEL);
endif

####
# Enable Early IO Release
ENABLE_EARLY_IO_RELEASE := 1
ifeq ($(ENABLE_EARLY_IO_RELEASE),1)
QUARTUS_TCL_ARGS += early_io_release 1
QSYS_TCL_CMDS += set early_io_release 1;
QUARTUS_CPF_ENABLE_SPLIT_RBF := 1
else
QUARTUS_CPF_ENABLE_SPLIT_RBF := 0
endif

####
# Enable PCIe
ENABLE_PCIE := 0
ifeq ($(ENABLE_PCIE),1)
QUARTUS_TCL_ARGS += fpga_pcie 1
QSYS_TCL_CMDS += set fpga_pcie 1;
endif

####
# Add SPIM0 to the FPGA. This is useful for testing
# the SPIM IP on the A10 dev kit in loopback mode
# since SPIM1 is connected to the Max V.
# SPIM1 will not be affected by this change.
# 1 is enabled
# 0 is disabled
SPIM0_EN ?= 0
ifeq ($(SPIM0_EN),1)
QSYS_TCL_CMDS += set spim0_en $(SPIM0_EN);
endif

####
# Graphics! Enable display port and frame reader
# 
# ENABLE_DISPLAY_PORT := 0
# ENABLE_FRAME_READER := $(ENABLE_DISPLAY_PORT)
#
# ifeq ($(ENABLE_DISPLAY_PORT),1)
# QUARTUS_TCL_ARGS += fpga_dp $(ENABLE_DISPLAY_PORT) 
# QSYS_TCL_CMDS += set fpga_dp $(ENABLE_DISPLAY_PORT);
# endif
#
# ifeq ($(ENABLE_FRAME_READER),1)
# QUARTUS_TCL_ARGS += frame_reader $(ENABLE_FRAME_READER)
# QSYS_TCL_CMDS += set frame_reader $(ENABLE_FRAME_READER);
# endif
####

####
# HPS Secure Boot

ENABLE_HPS_BOOTLOADER_SIGNING := 0
BOOTLOADER_SIGNING_KEY_TYPE := user
BOOTLOADER_SIGNING_KEY_FILE := root_key.pem

ENABLE_ENCRYPTION := 0
ENCRYPTION_KEY_FILE := encrypt.key
ENCRYPTION_KEY := key1

####
# Quartus Partial Reconfiguration
ENABLE_PARTIAL_RECONFIGURATION := 0
ENABLE_PARTIAL_RECONFIGURATION_TESTING := 0

ifeq ($(ENABLE_PARTIAL_RECONFIGURATION),1)
QUARTUS_TCL_ARGS += pr_enable $(ENABLE_PARTIAL_RECONFIGURATION) 
QSYS_TCL_CMDS += set pr_enable $(ENABLE_PARTIAL_RECONFIGURATION);
QSYS_TCL_CMDS += set freeze_ack_dly_enable $(ENABLE_PARTIAL_RECONFIGURATION_TESTING);

ifeq ($(HAVE_QUARTUS),1)
ifneq ($(HAVE_QUARTUS_PRO),1)
$(error ERROR: Partial Reconfiguration Designs require Quartus Prime PRO)
endif
ifneq ($(HAVE_QSYS_PRO),1)
$(error ERROR: Partial Reconfiguration Designs require QSys PRO)
endif
endif

# Add all named PR partitions
QUARTUS_PARTITIONS += pr_partition_0

endif # ENABLE_PARTIAL_RECONFIGURATION == 1
#### 


# Merge QSYS_TCL_CMDS into a single QSys arg
ifneq ($(QSYS_TCL_CMDS),)
QSYS_TCL_ARGS += --cmd="$(QSYS_TCL_CMDS)"
endif


#
# End of Design Settings
#
################################################



################################################
.PHONY: default
default: help
################################################

################################################
.PHONY: all
all: bootloader bootloader_ext_cfg dts dtb

ifeq ($(HAVE_QUARTUS),1)
all: rbf sof

ifeq ($(ENABLE_PARTIAL_RECONFIGURATION),1)
all: pr_rbf
endif
endif
################################################


################################################
# Target Stamping

SOCEDS_VERSION := $(if $(wildcard $(SOCEDS_DEST_ROOT)/version.txt),$(shell $(CAT) $(SOCEDS_DEST_ROOT)/version.txt 2>/dev/null | $(GREP) Version | $(HEAD) -n1 | $(SED) -e 's,^Version[: \t=]*\([0-9.]*\).*,\1,g' 2>/dev/null))

define get_stamp_dir
stamp$(if $(SOCEDS_VERSION),/$(SOCEDS_VERSION))
endef

define get_stamp_target
$(get_stamp_dir)$(if $1,/$1.stamp,$(error ERROR: Arg 1 missing to $0 function))
endef

define stamp_target
@$(MKDIR) $(@D)
@$(TOUCH) $@
endef

.PHONY: clean
clean:
	@$(ECHO) "Cleaning stamp files (which will trigger rebuild)"
	@$(RM) $(get_stamp_dir)
	@$(ECHO) " TIP: Use 'make scrub_clean' to get a deeper clean"
################################################


################################################
# Archiving & Cleaning your QuartusII/QSys Project

AR_TIMESTAMP := $(if $(SOCEDS_VERSION),$(subst .,_,$(SOCEDS_VERSION))_)$(subst $(SPACE),,$(shell $(DATE) +%m%d%Y_%k%M%S))

AR_DIR := tgz
AR_FILE := $(AR_DIR)/$(basename $(firstword $(wildcard *.qpf)))_$(AR_TIMESTAMP).tar.gz

SOFTWARE_DIR := software
BOOTLOADER_DIR := $(SOFTWARE_DIR)/bootloader
BOOTLOADER_EXT_CFG_DIR := $(SOFTWARE_DIR)/bootloader_ext_cfg

AR_REGEX += \
	Makefile ip readme.txt ds5 \
	altera_avalon* *.qpf *.qsf *.sdc *.v *.sv *.vhd *.qsys *.tcl *.terp *.stp *.mk \
	*.sed quartus.ini *.sof *.rbf *.sopcinfo */*.sopcinfo *.jdi output_files \
	hps_isw_handoff */*.svd */synthesis/*.svd */synth/*.svd *.dts *.dtb *.xml

AR_FILTER_OUT += %_tb.qsys
################################################



################################################
# Build QuartusII/QSys Project
#

#############
# QSys
QSYS_FILE_TOP := $(firstword $(wildcard *top*.qsys) $(wildcard ghrd*.qsys) $(wildcard *main*.qsys) $(wildcard *soc*.qsys) $(wildcard *.qsys))

OTHER_QSYS_FILES :=

QSYS_FILES += $(QSYS_FILE_TOP) $(OTHER_QSYS_FILES)

ifeq ($(QSYS_FILE_TOP),)
$(error ERROR: QSYS_FILE_TOP *.qsys file not set and could not be discovered)
endif

QSYS_DEPS += $(sort $(QSYS_FILES) $(filter-out pr_%.qsys,$(wildcard *.qsys)))

QSYS_BASE_TOP := $(basename $(QSYS_FILE_TOP))
QSYS_GEN_DIRS := $(foreach qsys_file,$(QSYS_FILES),$(dir $(qsys_file))$(notdir $(basename $(qsys_file))))

ifneq ($(HAVE_QUARTUS_PRO),1)
QSYS_QIP_FILES := $(foreach qsys_file,$(QSYS_FILES),$(dir $(qsys_file))$(notdir $(basename $(qsys_file)))/synthesis/$(basename $(notdir $(qsys_file))).qip)
else
QSYS_QIP_FILES := $(foreach qsys_file,$(QSYS_FILES),$(dir $(qsys_file))$(notdir $(basename $(qsys_file)))/$(basename $(notdir $(qsys_file))).qip)
endif


QSYS_SOPCINFO := $(QSYS_BASE_TOP)/$(QSYS_BASE_TOP).sopcinfo
QSYS_STAMP := $(foreach qsys_file,$(QSYS_FILES),$(call get_stamp_target,$(qsys_file).qsys_gen))

# Under cygwin, ensure TMP env variable is not a cygwin style path
# before calling qsys-generate
ifeq ($(IS_CYGWIN_HOST),1)
ifneq ($(shell $(WHICH) cygpath 2>/dev/null),)
SET_QSYS_GENERATE_ENV = TMP="$(shell cygpath -m "$(TMP)")"
endif
endif

.PHONY: qsys_compile
qsys_compile: $(QSYS_STAMP)

ifeq ($(HAVE_QSYS),1)
$(QSYS_SOPCINFO) $(QSYS_QIP_FILES): $(QSYS_STAMP)
	@if [ ! -f "$@" ]; then echo "ERROR: $@ not generated" && false; fi
	$(stamp_target)
endif

ifeq ($(HAVE_QSYS_PRO),1)
QSYS_ARGS += --pro
QSYS_ARGS += --quartus-project=$(QUARTUS_QPF)
QSYS_ARGS += --rev=$(QUARTUS_BASE_REVISION)
QSYS_GENERATE_ARGS += --pro
QSYS_GENERATE_ARGS += --clear-output-directory
endif

$(QSYS_STAMP): $(get_stamp_dir)/%.qsys_gen.stamp: % $(QSYS_DEPS)
	$(SET_QSYS_GENERATE_ENV) qsys-generate $(QSYS_GENERATE_ARGS) $< --upgrade-ip-cores
	$(SET_QSYS_GENERATE_ENV) qsys-script --qpf=none --script=update_sysid.tcl --system-file=$<
	$(SET_QSYS_GENERATE_ENV) qsys-generate $(QSYS_GENERATE_ARGS) $< --synthesis=VERILOG
	$(stamp_target)

HELP_TARGETS += qsys_edit

qsys_edit.HELP := Launch QSys GUI
ifneq ($(HAVE_QSYS),1)
qsys_edit.HELP := $(qsys_edit.HELP) (Install Quartus II Software to enable)
endif

.PHONY: qsys_edit
qsys_edit:
	qsys-edit $(QSYS_ARGS) $(QSYS_FILE_TOP) &


SCRUB_CLEAN_FILES += $(wildcard .qsys_edit)

ifeq ($(HAVE_QSYS),1)
SCRUB_CLEAN_FILES += $(QSYS_QIP_FILES) $(QSYS_SOPCINFO) $(QSYS_GEN_DIRS)
endif

#############
# Quartus II

QUARTUS_QPF := $(firstword $(wildcard *.qpf))
ifeq ($(QUARTUS_QPF),)
$(error ERROR: QUARTUS_QPF *.qpf file not set and could not be discovered)
endif
QUARTUS_BASE := $(basename $(QUARTUS_QPF))
QUARTUS_HDL_SOURCE := $(wildcard *.v *.sv *.vhd)
QUARTUS_MISC_SOURCE := $(wildcard *.stp *.sdc)
QUARTUS_BASE_REVISION := $(QUARTUS_BASE)
QUARTUS_QSF := $(QUARTUS_BASE_REVISION).qsf

QUARTUS_DEPS += $(QUARTUS_QSF) $(QUARTUS_HDL_SOURCE) $(QUARTUS_MISC_SOURCE) $(QSYS_STAMP) $(QSYS_QIP_FILES)

QUARTUS_OUTPUT_DIR := output_files
QUARTUS_SOF := $(QUARTUS_OUTPUT_DIR)/$(QUARTUS_BASE).sof
QUARTUS_STAMP := $(call get_stamp_target,quartus)

.PHONY: quartus_compile
quartus_compile: $(QUARTUS_STAMP)

ifeq ($(HAVE_QUARTUS),1)
$(QUARTUS_SOF): $(QUARTUS_STAMP)
endif

$(QUARTUS_STAMP): $(QUARTUS_DEPS)
	quartus_stp $(QUARTUS_BASE) -c $(QUARTUS_BASE_REVISION)
	quartus_sh --flow compile $(QUARTUS_QPF) -c $(QUARTUS_BASE_REVISION)
	$(stamp_target)

HELP_TARGETS += quartus_edit
quartus_edit.HELP := Launch Quartus Prime GUI

ifneq ($(HAVE_QUARTUS),1)
quartus_edit.HELP := $(quartus_edit.HELP) (Install Quartus II Software to enable)
endif

.PHONY: quartus_edit
quartus_edit:
	quartus $(QUARTUS_QPF) &

HELP_TARGETS += sof
sof.HELP := QSys generate & Quartus compile this design
ifneq ($(HAVE_QUARTUS),1)
sof.HELP := $(sof.HELP) (Install Quartus II Software to enable)
endif

BATCH_TARGETS += sof

.PHONY: sof
sof: $(QUARTUS_SOF)

#
# This converts the sof into compressed, unencrypted 
# raw binary format
#
ifeq ($(QUARTUS_CPF_ENABLE_SPLIT_RBF),1)

QUARTUS_CPF_ARGS += --hps

ifeq ($(ENABLE_ENCRYPTION),1)
QUARTUS_CORE_RBF := $(patsubst %.sof,%-encrypted.core.rbf,$(QUARTUS_SOF))
QUARTUS_PERIPH_RBF := $(patsubst %.sof,%-encrypted.periph.rbf,$(QUARTUS_SOF))
else # if encryption not enabled
QUARTUS_CORE_RBF := $(patsubst %.sof,%.core.rbf,$(QUARTUS_SOF))
QUARTUS_PERIPH_RBF := $(patsubst %.sof,%.periph.rbf,$(QUARTUS_SOF))
endif # ENABLE_ENCRYPTION == 1

QUARTUS_RBF := $(QUARTUS_CORE_RBF) $(QUARTUS_PERIPH_RBF)

else # if split rbf not enabled

ifeq ($(ENABLE_ENCRYPTION),1)
QUARTUS_RBF := $(patsubst %.sof,%-encrypted.rbf,$(QUARTUS_SOF))
else # if encryption not enabled
QUARTUS_RBF := $(patsubst %.sof,%.rbf,$(QUARTUS_SOF))
endif # ENABLE_ENCRYPTION == 1

endif # QUARTUS_CPF_ENABLE_SPLIT_RBF == 1


# Partial Reconfig RBF build flow
ifeq ($(ENABLE_PARTIAL_RECONFIGURATION),1)

QUARTUS_PR_REVISIONS += alternate_persona

QUARTUS_PR_RBF := $(strip \
   $(foreach revision,$(QUARTUS_BASE_REVISION) $(QUARTUS_PR_REVISIONS),\
      $(foreach partition,$(QUARTUS_PARTITIONS),\
          $(QUARTUS_OUTPUT_DIR)/$(revision).$(partition).rbf)))

QUARTUS_PMSF := $(patsubst %.rbf,%.pmsf,$(QUARTUS_PR_RBF))

endif

#####################

#####################
QUARTUS_BASE_QDB := $(QUARTUS_BASE).qdb
SCRUB_CLEAN_FILES += $(QUARTUS_BASE_QDB)

.PHONY: qdb
qdb: $(QUARTUS_BASE_QDB)

$(QUARTUS_BASE_QDB): $(QUARTUS_STAMP)
	quartus_cdb $(QUARTUS_QPF) -c $(QUARTUS_BASE_REVISION) --export_pr_static root_partition --snapshot final --file $@

ifeq ($(ENABLE_PARTIAL_RECONFIGURATION),1)


QUARTUS_SYNTH_REVISION := synth_v2
QUARTUS_PR_REVISION := alternate_persona
PARTITION_NAME := pr_partition_0
QSYS_SUBSYS_PR := pr_region_0_persona


.PHONY: quartus_generate_pr_qsf
quartus_generate_pr_qsf: $(QUARTUS_SYNTH_REVISION).qsf $(QUARTUS_PR_REVISION).qsf

.PHONY: qsys_generate_pr_qsys
qsys_generate_pr_qsys: $(QSYS_SUBSYS_PR).qsys

$(QUARTUS_SYNTH_REVISION).qsf: %.qsf: create_pr_revision.tcl
	quartus_sh --script=$< -projectname $(QUARTUS_BASE) -revision $(QUARTUS_BASE_REVISION) -pr_revision $* -revision_type synthesis -pr_partition $(QSYS_SUBSYS_PR)

$(QUARTUS_PR_REVISION).qsf: %.qsf: create_pr_revision.tcl
	quartus_sh --script=$< -projectname $(QUARTUS_BASE) -revision $(QUARTUS_BASE_REVISION) -pr_revision $* 

$(QSYS_SUBSYS_PR).qsys: %.qsys: construct_subsys_pr_region.tcl $(QUARTUS_SYNTH_REVISION).qsf
	qsys-script --pro --script=$< --quartus-project=$(QUARTUS_BASE) --rev=$(QUARTUS_SYNTH_REVISION) --cmd="set sub_qsys_pr $(QSYS_SUBSYS_PR); set pr_persona 1"
	$(stamp_target)

$(QSYS_SUBSYS_PR)/$(QSYS_SUBSYS_PR).qip: $(QSYS_SUBSYS_PR).qsys
	$(SET_QSYS_GENERATE_ENV) qsys-generate $(QSYS_GENERATE_ARGS) --quartus-project=$(QUARTUS_BASE) --rev=$(QUARTUS_SYNTH_REVISION) $< --upgrade-ip-cores
	$(SET_QSYS_GENERATE_ENV) qsys-script --qpf=none --script=update_sysid.tcl --system-file=$<
	$(SET_QSYS_GENERATE_ENV) qsys-generate $(QSYS_GENERATE_ARGS) --quartus-project=$(QUARTUS_BASE) --rev=$(QUARTUS_SYNTH_REVISION) $< --synthesis=VERILOG

$(QUARTUS_SYNTH_REVISION).qdb: %.qdb: $(QUARTUS_BASE_QDB) $(QUARTUS_SYNTH_REVISION).qsf $(QSYS_SUBSYS_PR)/$(QSYS_SUBSYS_PR).qip
	quartus_syn $(QUARTUS_QPF) -c $*
	quartus_cdb $(QUARTUS_QPF) -c $* --export_block root_partition --snapshot synthesized --file $@

$(QUARTUS_OUTPUT_DIR)/$(QUARTUS_PR_REVISION).$(PARTITION_NAME).pmsf: %.pmsf: $(QUARTUS_BASE_QDB) $(QUARTUS_SYNTH_REVISION).qdb $(QUARTUS_PR_REVISION).qsf
	quartus_cdb $(QUARTUS_QPF) -c $(QUARTUS_PR_REVISION) --import_block root_partition --file $(QUARTUS_BASE_QDB)
	quartus_cdb $(QUARTUS_QPF) -c $(QUARTUS_PR_REVISION) --import_block $(PARTITION_NAME) --file $(QUARTUS_SYNTH_REVISION).qdb
	quartus_fit $(QUARTUS_QPF) -c $(QUARTUS_PR_REVISION)
	quartus_sta $(QUARTUS_QPF) -c $(QUARTUS_PR_REVISION)
	quartus_asm $(QUARTUS_QPF) -c $(QUARTUS_PR_REVISION)

$(QUARTUS_OUTPUT_DIR)/$(QUARTUS_BASE_REVISION).$(PARTITION_NAME).pmsf: $(QUARTUS_SOF)

endif


#####################

ifeq ($(ENABLE_ENCRYPTION),1)
QUARTUS_CPF_ENABLE_COMPRESSION := 0
QUARTUS_CPF_ENCRYPT_ARGS := --key "$(BOOTLOADER_DIR)/$(ENCRYPTION_KEY_FILE):$(ENCRYPTION_KEY)"
QUARTUS_CPF_DEPS += $(BOOTLOADER_DIR)/$(ENCRYPTION_KEY_FILE)
QUARTUS_CPF_ARGS += $(QUARTUS_CPF_ENCRYPT_ARGS)
endif

QUARTUS_CPF_ENABLE_COMPRESSION ?= 1
ifeq ($(QUARTUS_CPF_ENABLE_COMPRESSION),1)
QUARTUS_CPF_ARGS += -o bitstream_compression=on
endif

#######
# ENCRYPTION Sanity Checks
ifeq ($(ENABLE_ENCRYPTION),1)
ifeq ($(QUARTUS_CPF_ENABLE_COMPRESSION),1)
$(error ERROR: ENCRYPTION and COMPRESSION cannot be enabled simultaneously)
endif
ifeq ($(IS_WINDOWS_HOST),1)
$(error ERROR: Bootloader Encryption is not supported on Windows Host)
endif
endif
#######

ifeq ($(HAVE_QUARTUS),1)
QUARTUS_RBF_STAMP := $(call get_stamp_target,quartus_rbf)
$(QUARTUS_RBF_STAMP): $(QUARTUS_STAMP)
endif

.PHONY: rbf
rbf: $(QUARTUS_RBF)

$(QUARTUS_RBF): $(QUARTUS_RBF_STAMP)

QUARTUS_CPF_RBF_ARG := $(patsubst %.sof,%.rbf,$(QUARTUS_SOF))
ifeq ($(ENABLE_ENCRYPTION),1)
QUARTUS_CPF_RBF_ARG :=  $(patsubst %.rbf,%-encrypted.rbf,$(QUARTUS_CPF_RBF_ARG))
endif

$(QUARTUS_RBF_STAMP): $(QUARTUS_CPF_DEPS)
	@$(MKDIR) $(dir $(QUARTUS_RBF))
	quartus_cpf --convert $(QUARTUS_CPF_ARGS) $(QUARTUS_SOF) $(QUARTUS_CPF_RBF_ARG)
	$(stamp_target)

#ifeq ($(QUARTUS_CPF_ENABLE_COMPRESSION),1)
#QUARTUS_PR_CPF_ARGS += -o bitstream_compression=on
#endif

.PHONY: pr_rbf
pr_rbf: $(QUARTUS_PR_RBF)

$(QUARTUS_PR_RBF): %.rbf: %.pmsf
	quartus_cpf --convert $(QUARTUS_PR_CPF_ARGS) $< $@

ifeq ($(ENABLE_ENCRYPTION),1)
QUARTUS_EKP := $(patsubst %.sof,%.ekp,$(QUARTUS_SOF))
$(QUARTUS_RBF): $(QUARTUS_EKP)
.PHONY: ekp
$(QUARTUS_EKP) ekp: $(QUARTUS_CPF_DEPS)
	quartus_cpf $(QUARTUS_CPF_ENCRYPT_ARGS) $(QUARTUS_SOF) $(QUARTUS_EKP)
endif


ifeq ($(HAVE_QUARTUS),1)
SCRUB_CLEAN_FILES += $(QUARTUS_SOF) $(QUARTUS_RBF) $(QUARTUS_PR_RBF) $(QUARTUS_OUTPUT_DIR) hps_isw_handoff $(QUARTUS_EKP)
endif

################################################


################################################
# QSYS/Quartus Project Generation
#  - we don't run this generation step automatically because 
#    it will destroy any changes and/or customizations that 
#    you've made to your qsys, top level hdl or your quartus 
#    project
#
QSYS_QSYS_GEN := $(firstword $(wildcard create_*_qsys.tcl))
QUARTUS_TOP_GEN := $(firstword $(wildcard create_*_top.tcl))
QUARTUS_QSF_QPF_GEN := $(firstword $(wildcard create_*_quartus.tcl))

.PHONY: quartus_generate_qsf_qpf
ifneq ($(QUARTUS_QSF_QPF_GEN),)
quartus_generate_qsf_qpf: $(QUARTUS_QSF_QPF_GEN)
	@$(RM) $(QUARTUS_QSF) $(QUARTUS_QPF)
	quartus_sh --script=$< $(QUARTUS_TCL_ARGS)
else
quartus_generate_qsf_qpf:
	@$(ECHO) "Make target '$@' is not supported for this design"
endif

.PHONY: quartus_generate_top
ifneq ($(QUARTUS_TOP_GEN),)
quartus_generate_top: $(QUARTUS_TOP_GEN)
	@$(RM) *_top.v
	quartus_sh --script=$< $(QUARTUS_TCL_ARGS)
else
quartus_generate_top:
	@$(ECHO) "Make target '$@' is not supported for this design"
endif

ifeq ($(HAVE_QSYS_PRO),1)
QSYS_GEN_QSYS_DEPS += quartus_generate_qsf_qpf
endif

.PHONY: qsys_generate_qsys
ifneq ($(QSYS_QSYS_GEN),)
qsys_generate_qsys: $(QSYS_QSYS_GEN) $(QSYS_GEN_QSYS_DEPS)
	@$(RM) $(QSYS_FILE_TOP)
	qsys-script $(QSYS_ARGS) --script=$< $(QSYS_TCL_ARGS)
	$(stamp_target)
else
qsys_generate_qsys:
	@$(ECHO) "Make target '$@' is not supported for this design"
endif

HELP_TARGETS += generate_from_tcl
generate_from_tcl.HELP := Generate the Quartus Project source files from tcl script source

.PHONY: generate_from_tcl
generate_from_tcl:
	$(MAKE) -s scrub_clean
	$(MAKE) quartus_generate_qsf_qpf quartus_generate_top qsys_generate_qsys
ifeq ($(ENABLE_PARTIAL_RECONFIGURATION),1)
	$(MAKE) quartus_generate_pr_qsf qsys_generate_pr_qsys
endif

################################################


################################################
# Quartus Programming
QUARTUS_PGM_STAMP := $(call get_stamp_target,quartus_pgm)

# set this correctly for your board. If not set, assume the first board
# is your board
CLOSE_PARENTHESIS = )
BOARD_CABLE ?= $(shell jtagconfig | head -n1 | sed -e 's,[0-9]*[$(CLOSE_PARENTHESIS)][ \t]*,,')

# FPGA Board Device Index. Default to 1 since this is the most
#  common setting for a10 dev board
BOARD_DEVICE_INDEX ?= 1

define quartus_pgm_sof
jtagconfig
quartus_pgm --mode=jtag $(if $(BOARD_CABLE),--cable="$(BOARD_CABLE)") --operation=p\;$1$(if $(BOARD_DEVICE_INDEX),"@$(BOARD_DEVICE_INDEX)")
jtagconfig $(if $(BOARD_CABLE),-c "$(BOARD_CABLE)") -n
endef

.PHONY: pgm
pgm: $(QUARTUS_PGM_STAMP)

$(QUARTUS_PGM_STAMP): $(QUARTUS_SOF)
	$(call quartus_pgm_sof,$<)
	$(stamp_target)

HELP_TARGETS += program_fpga
program_fpga.HELP := Quartus program sof to your attached dev board

.PHONY: program_fpga
program_fpga:
	$(call quartus_pgm_sof,$(QUARTUS_SOF))
################################################


################################################
# Bootloader

QSYS_HPS_INST_NAME ?= hps_0

SBT.CREATE_SETTINGS := bsp-create-settings
SBT.GENERATE := bsp-generate-files

BOOTLOADER_ID := hps_isw_handoff/id
BOOTLOADER_DEPS += $(BOOTLOADER_ID)

ifeq ($(HAVE_QUARTUS),1)
BOOTLOADER_DEPS += $(QUARTUS_STAMP)

$(BOOTLOADER_ID): $(QUARTUS_STAMP) 
endif

BOOTLOADER_STAMP := $(call get_stamp_target,bootloader)
BOOTLOADER_EXT_CFG_STAMP := $(call get_stamp_target,bootloader_ext_cfg)

ifneq ($(HPS_BOOT_DEVICE),)
BOOTLOADER_EXTRA_ARGS += --set uboot.boot_device $(HPS_BOOT_DEVICE)
endif

ifeq ($(HPS_BOOT_DEVICE),SDMMC)

# These settings only apply to SDMMC
# DTS needs to specify the file names of the rbf files so that
# the bootloader can find them on the FAT partition

ifeq ($(QUARTUS_CPF_ENABLE_SPLIT_RBF),1)
ifneq ($(QUARTUS_PERIPH_RBF),)
BOOTLOADER_EXTRA_ARGS += --set uboot.rbf_filename $(notdir $(QUARTUS_PERIPH_RBF))
endif
else # QUARTUS_CPF_ENABLE_SPLIT_RBF != 1
ifneq ($(QUARTUS_RBF),)
BOOTLOADER_EXTRA_ARGS += --set uboot.rbf_filename $(notdir $(QUARTUS_RBF))
endif
endif


all: sd-fat

else # if HPS_BOOT_DEVICE is not SDMMC

# when not building for SDMMC, a mkimage wrapped rbf file is required 
# (supported on linux only because uboot build of mkimage is not 
# supported on windows yet)
ifneq ($(IS_WINDOWS_HOST),1)
all: rbf_mkimage
endif # IS_WINDOWS_HOST == 1

BOOTLOADER_EXTRA_ARGS += --set uboot.rbf_filename ""

endif # HPS_BOOT_DEVICE == SDMMC


###################################
# These bootloader settings are deprecated now. 
# Set them to an empty string to ensure
# they don't get used by accident
BOOTLOADER_EXTRA_ARGS += --set uboot.core_rbf_filename ""
BOOTLOADER_EXTRA_ARGS += --set uboot.peripheral_rbf_filename ""
###################################


ifeq ($(ENABLE_HPS_BOOTLOADER_SIGNING),1)
BOOTLOADER_EXTRA_ARGS += --set uboot.secureboot.enable_bootloader_signing true
BOOTLOADER_EXTRA_ARGS += --set uboot.secureboot.signing_key_type $(BOOTLOADER_SIGNING_KEY_TYPE)
BOOTLOADER_EXTRA_ARGS += --set uboot.secureboot.signing_key_pair_file $(BOOTLOADER_SIGNING_KEY_FILE)
endif

ifeq ($(ENABLE_ENCRYPTION),1)
BOOTLOADER_EXTRA_ARGS += --set uboot.secureboot.enable_bootloader_encryption true
BOOTLOADER_EXTRA_ARGS += --set uboot.secureboot.encryption_key_file $(ENCRYPTION_KEY_FILE)
BOOTLOADER_EXTRA_ARGS += --set uboot.secureboot.encryption_key_name $(ENCRYPTION_KEY)

ifeq ($(wildcard $(BOOTLOADER_DIR)/$(ENCRYPTION_KEY_FILE)),)
$(BOOTLOADER_DIR)/$(ENCRYPTION_KEY_FILE): $(BOOTLOADER_STAMP)
endif

endif

ifeq ($(IS_WINDOWS_HOST),1)
BOOTLOADER_EXTRA_ARGS += --set uboot.disable_uboot_build true
endif

.PHONY: bootloader
bootloader: $(BOOTLOADER_STAMP)

# Create and build bootloader
$(BOOTLOADER_STAMP): $(BOOTLOADER_DEPS)
	@$(MKDIR) $(BOOTLOADER_DIR)
	$(SBT.CREATE_SETTINGS) \
		--type uboot \
		--bsp-dir $(BOOTLOADER_DIR) \
		--preloader-settings-dir "hps_isw_handoff" \
		--settings $(BOOTLOADER_DIR)/settings.bsp \
		$(BOOTLOADER_EXTRA_ARGS)

ifeq ($(ENABLE_HPS_BOOTLOADER_SIGNING),1)
ifeq ($(wildcard $(BOOTLOADER_DIR)/$(BOOTLOADER_SIGNING_KEY_FILE)),)
	$(MAKE) -C $(BOOTLOADER_DIR) generate-signing-key-pair-file
endif
endif

ifeq ($(ENABLE_ENCRYPTION),1)
ifeq ($(wildcard $(BOOTLOADER_DIR)/$(ENCRYPTION_KEY_FILE)),)
	$(MAKE) -C $(BOOTLOADER_DIR) generate-example-encryption-key-file
endif
endif

	$(MAKE) -C $(BOOTLOADER_DIR)
	$(stamp_target)

AR_REGEX += $(BOOTLOADER_DIR)

.PHONY: bootloader_ext_cfg
bootloader_ext_cfg: $(BOOTLOADER_EXT_CFG_STAMP)

$(BOOTLOADER_EXT_CFG_STAMP): $(BOOTLOADER_DEPS)
	@$(MKDIR) $(BOOTLOADER_EXT_CFG_DIR)
	$(SBT.CREATE_SETTINGS) \
		--type uboot \
		--bsp-dir $(BOOTLOADER_EXT_CFG_DIR) \
		--preloader-settings-dir "hps_isw_handoff" \
		--settings $(BOOTLOADER_EXT_CFG_DIR)/settings.bsp \
		--set uboot.external_fpga_config true \
		$(BOOTLOADER_EXTRA_ARGS)

ifeq ($(ENABLE_HPS_BOOTLOADER_SIGNING),1)
ifeq ($(wildcard $(BOOTLOADER_EXT_CFG_DIR)/$(BOOTLOADER_SIGNING_KEY_FILE)),)
	$(MAKE) -C $(BOOTLOADER_EXT_CFG_DIR) generate-signing-key-pair-file
endif
endif

ifeq ($(ENABLE_ENCRYPTION),1)
ifeq ($(wildcard $(BOOTLOADER_EXT_CFG_DIR)/$(ENCRYPTION_KEY_FILE)),)
	$(MAKE) -C $(BOOTLOADER_EXT_CFG_DIR) generate-example-encryption-key-file
endif
endif

	$(MAKE) -C $(BOOTLOADER_EXT_CFG_DIR)
	$(stamp_target)

AR_REGEX += $(BOOTLOADER_EXT_CFG_DIR)

HELP_TARGETS += uboot
uboot.HELP := Build U-Boot into $(BOOTLOADER_DIR) directory

.PHONY: uboot
uboot: $(BOOTLOADER_STAMP)

BOOTLOADER_BIN := $(BOOTLOADER_DIR)/uboot_w_dtb-mkpimage.bin 
ifeq ($(ENABLE_ENCRYPTION),1)
BOOTLOADER_BIN := $(patsubst %.bin,%-encrypted.abin,$(BOOTLOADER_BIN))
ifeq ($(ENABLE_HPS_BOOTLOADER_SIGNING),1)
BOOTLOADER_BIN := $(patsubst %.abin,%-signed.abin,$(BOOTLOADER_BIN))
endif
BOOTLOADER_BIN := $(patsubst %.abin,%-x4.abin,$(BOOTLOADER_BIN))
else # ENABLE_ENCRYPTION != 1
ifeq ($(ENABLE_HPS_BOOTLOADER_SIGNING),1)
BOOTLOADER_BIN := $(patsubst %.bin,%-signed-x4.abin,$(BOOTLOADER_BIN))
endif
endif # ENABLE_ENCRYPTION == 1

SCRUB_CLEAN_FILES += $(BOOTLOADER_DIR) $(BOOTLOADER_EXT_CFG_DIR)

ifeq ($(IS_WINDOWS_HOST),1)
EXE_EXT := .exe
endif
UBOOT_MKIMAGE := $(BOOTLOADER_DIR)/uboot-socfpga/tools/mkimage$(EXE_EXT)
AR_REGEX += $(UBOOT_MKIMAGE)

################################################


################################################
# Linux (OS) Device Tree

DTS.SOPC2DTS := sopc2dts
DTS.DTC := dtc

DTS.BOARDINFO ?= $(QSYS_BASE_TOP)_board_info.xml
DTS.COMMON ?= $(wildcard hps*common_board_info.xml) $(wildcard hps*devkit_board_info.xml)

DTS.EXTRA_DEPS += $(DTS.BOARDINFO) $(DTS.COMMON)

# boardinfo xml order matters
DTS.SOPC2DTS_ARGS += $(if $(DTS.COMMON),$(patsubst %,--board %,$(DTS.COMMON)))
DTS.SOPC2DTS_ARGS += $(if $(DTS.BOARDINFO),--board $(DTS.BOARDINFO))
DTS.SOPC2DTS_ARGS += --bridge-removal all
DTS.SOPC2DTS_ARGS += --clocks

define dts.sopc2dts
$(if $(DTS.BOARDINFO),,$(warning WARNING: DTS BoardInfo file was not specified or found))
$(DTS.SOPC2DTS) --input $1 --output $2 $3 $(DTS.SOPC2DTS_ARGS)
endef

# Device Tree Source (dts)
DEVICE_TREE_SOURCE := $(QSYS_BASE_TOP).dts

HELP_TARGETS += dts
dts.HELP := Generate a device tree for this qsys design

.PHONY: dts
dts: $(DEVICE_TREE_SOURCE)

ifeq ($(HAVE_QSYS),1)
$(DEVICE_TREE_SOURCE): $(QSYS_STAMP)
endif

$(DEVICE_TREE_SOURCE): $(QSYS_SOPCINFO) $(DTS.EXTRA_DEPS)
	$(call dts.sopc2dts,$<,$@)


# Device Tree Blob (dtb)
DEVICE_TREE_BLOB := $(QSYS_BASE_TOP).dtb

HELP_TARGETS += dtb
dtb.HELP := Generate a device tree blob for this qsys design

.PHONY: dtb
dtb: $(DEVICE_TREE_BLOB)

ifeq ($(HAVE_QSYS),1)
$(DEVICE_TREE_BLOB): $(QSYS_STAMP)
endif

$(DEVICE_TREE_BLOB): %.dtb: %.dts
	$(DTS.DTC) -I dts -O dtb -o $@ $<

SCRUB_CLEAN_FILES += $(DEVICE_TREE_SOURCE) $(DEVICE_TREE_BLOB)

################################################


################################################
ifneq ($(QUARTUS_CPF_ENABLE_SPLIT_RBF),1)
QUARTUS_RBF_MKIMAGE := $(patsubst %.rbf,%.rbf.mkimage,$(QUARTUS_RBF))
else
QUARTUS_PERIPH_RBF_MKIMAGE := $(patsubst %.rbf,%.rbf.mkimage,$(QUARTUS_PERIPH_RBF))
QUARTUS_CORE_RBF_MKIMAGE := $(patsubst %.rbf,%.rbf.mkimage,$(QUARTUS_CORE_RBF))
QUARTUS_RBF_MKIMAGE := $(QUARTUS_PERIPH_RBF_MKIMAGE) $(QUARTUS_CORE_RBF_MKIMAGE)
endif

AR_REGEX += $(QUARTUS_RBF_MKIMAGE)

.PHONY: rbf_mkimage
rbf_mkimage: $(QUARTUS_RBF_MKIMAGE) $(BOOTLOADER_STAMP)

$(QUARTUS_RBF_MKIMAGE): %.rbf.mkimage: %.rbf
	$(UBOOT_MKIMAGE) -A arm -T firmware -C none -O u-boot -a 0 -e 0 -n "RBF" -d $< $@


SCRUB_CLEAN_FILES += $(QUARTUS_RBF_MKIMAGE)
################################################


################################################
# Quartus Flash Programming QSPI
# HPS Device Index. Default to 2 since this is the most
#  common setting for dev board
BOARD_HPS_DEVICE_INDEX ?= 2

define quartus_hps_pgm_qspi
jtagconfig
quartus_hps $(if $(BOARD_CABLE),--cable="$(BOARD_CABLE)") $(if $(BOARD_HPS_DEVICE_INDEX),--device=$(BOARD_HPS_DEVICE_INDEX)) --operation=PV --addr=$1 $2
endef

define quartus_hps_pgm_erase_qspi
jtagconfig
quartus_hps $(if $(BOARD_CABLE),--cable="$(BOARD_CABLE)") $(if $(BOARD_HPS_DEVICE_INDEX),--device=$(BOARD_HPS_DEVICE_INDEX)) --operation=E --addr=$1 --size=$2
endef

# The program_qspi target assumes QUARTUS_CPF_ENABLE_SPLIT_RBF != 1
# This is the assumed qspi memory map:
#   [Values can be changed by editing the U-Boot source code file 
#    include/configs/socfpga_arria10.h]
QSPI_BOOTLOADER_ADDR  = 0x00000000
QSPI_FTD_ADDR        ?= 0x00100000
QSPI_BOOTIMAGE_ADDR  ?= 0x00120000
QSPI_RBF_ADDR        ?= 0x00720000
QSPI_PERIPH_RBF_ADDR ?= 0x00720000
QSPI_CORE_RBF_ADDR   ?= 0x00820000

# These rootfs address and size values come from the linux device tree 
# [see hps_a10_devkit_board_info.xml]
QSPI_ROOTFS_ADDR    ?= 0x03020000
QSPI_ROOTFS_SIZE    ?= 0x04FE0000

QSPI_BOOTLOADER = $(BOOTLOADER_BIN)
QSPI_BOOTIMAGE ?= $(SOCEDS_DEST_ROOT)/embeddedsw/socfpga/prebuilt_images/zImage
QSPI_ROOTFS ?= $(SOCEDS_DEST_ROOT)/embeddedsw/socfpga/prebuilt_images/angstrom-minimal-rootfs-qspi-64k-erase.jffs2

HELP_TARGETS += program_qspi
program_qspi.HELP := Flash program the bootloader, rbf, boot image, and root filesystem into QSPI Flash

.PHONY: program_qspi
program_qspi: program_qspi-bootloader program_qspi-rbf program_qspi-linux-dtb program_qspi-bootimage program_qspi-rootfs

.PHONY: program_qspi-bootloader
program_qspi-bootloader:
	$(call quartus_hps_pgm_qspi,$(QSPI_BOOTLOADER_ADDR),$(QSPI_BOOTLOADER))

.PHONY: program_qspi-rbf
program_qspi-rbf:
ifneq ($(QUARTUS_CPF_ENABLE_SPLIT_RBF),1)
	$(call quartus_hps_pgm_qspi,$(QSPI_RBF_ADDR),$(QUARTUS_RBF_MKIMAGE))
else
	$(call quartus_hps_pgm_qspi,$(QSPI_PERIPH_RBF_ADDR),$(QUARTUS_PERIPH_RBF_MKIMAGE))
	$(call quartus_hps_pgm_qspi,$(QSPI_CORE_RBF_ADDR),$(QUARTUS_CORE_RBF_MKIMAGE))
endif

.PHONY: program_qspi-linux-dtb
program_qspi-linux-dtb:
	$(call quartus_hps_pgm_qspi,$(QSPI_FTD_ADDR),$(DEVICE_TREE_BLOB))

.PHONY: program_qspi-bootimage
program_qspi-bootimage:
ifneq ($(QSPI_BOOTIMAGE),)
	$(call quartus_hps_pgm_qspi,$(QSPI_BOOTIMAGE_ADDR),$(QSPI_BOOTIMAGE))
else
	$(warning WARNING: QSPI_BOOTIMAGE variable not defined)
endif

.PHONY: program_qspi-rootfs
program_qspi-rootfs:
ifneq ($(QSPI_ROOTFS),)
	$(call quartus_hps_pgm_erase_qspi,$(QSPI_ROOTFS_ADDR),$(QSPI_ROOTFS_SIZE))
	$(call quartus_hps_pgm_qspi,$(QSPI_ROOTFS_ADDR),$(QSPI_ROOTFS))
else
	$(warning WARNING: QSPI_ROOTFS variable not defined)
endif


################################################


################################################
# Quartus Flash Programming NAND
# 
# NAND flash programming with quartus_hps is not yet supported
#

.PHONY: program_nand
program_nand:
	$(error ERROR: $@ is not yet supported)

################################################


################################################
# Bootloader SD Card Programming

# Update the A2 Partition on your sd card with
# the bootloader that's built with this design

# These targets assume you have a pre-imaged sd card
# or an sd card *.img file 
# An example sd image for the Altera SoC Development
# Board can be found here:
# <soceds_install>/embeddedsw/socfpga/prebuilt_images

ALT_BOOT_DISK_UTIL := alt-boot-disk-util

ifeq ($(IS_WINDOWS_HOST),1)

ifeq ($(SDCARD),)
ifeq ($(SD_DRIVE_LETTER),)
GUESS_DRIVE_LETTER = $(firstword $(foreach drive_letter,d e f g h i j k l m n o p q r s t u v w x y z,$(if $(wildcard $(drive_letter):/zImage),$(drive_letter))))
SD_DRIVE_LETTER = $(GUESS_DRIVE_LETTER)
endif # SD_DRIVE_LETTER == <empty>
SDCARD ?= $(if $(SD_DRIVE_LETTER),-d $(SD_DRIVE_LETTER),$(error ERROR: SD_DRIVE_LETTER not specified. Try "make $(MAKECMDGOALS) SD_DRIVE_LETTER=[sd_card_windows_drive_letter]"))
endif # SDCARD == <empty>

else # if not a Windows Host

SDCARD ?= $(error ERROR: SD Card not specified. Try "make $(MAKECMDGOALS) SDCARD=/dev/sdX", where X represents your target SD Card device)

endif

.PHONY: sd-update-bootloader
sd-update-bootloader: $(BOOTLOADER_BIN)
	$(ALT_BOOT_DISK_UTIL) -B $< -a write $(SDCARD)

################################################


################################################
# GHRD HPS Reset Targets
ifneq ($(wildcard ghrd_reset.tcl),)
# use the already programmed fpga to reset the hps
HPS_RESET_TARGETS := hps_cold_reset hps_warm_reset hps_debug_reset

.PHONY: $(HPS_RESET_TARGETS) 
$(HPS_RESET_TARGETS): hps_%_reset:
	quartus_stp --script=ghrd_reset.tcl $(if $(BOARD_CABLE),--cable-name "$(BOARD_CABLE)") $(if $(BOARD_DEVICE_INDEX),--device-index "$(BOARD_DEVICE_INDEX)") --$*-reset
endif

################################################


################################################

SD_FAT_TGZ ?= sd_fat.tar.gz
SD_FAT_TGZ_STAGE_DIR = $(patsubst %.tar.gz,%_tar_gz,$(SD_FAT_TGZ))_stage_dir
SD_FAT_TGZ_DEPS += $(QUARTUS_RBF) $(QUARTUS_PR_RBF) $(DEVICE_TREE_BLOB)

$(SD_FAT_TGZ): $(SD_FAT_TGZ_DEPS)
	@$(RM) $@ $(SD_FAT_TGZ_STAGE_DIR)
	@$(MKDIR) $(@D)
	@$(MKDIR) $(SD_FAT_TGZ_STAGE_DIR)
	$(CP) $^ $(SD_FAT_TGZ_STAGE_DIR)
	cd $(SD_FAT_TGZ_STAGE_DIR) && $(TAR) czf ../$(notdir $@) *
	@$(RM) $(SD_FAT_TGZ_STAGE_DIR)

.PHONY: sd-fat
sd-fat: $(SD_FAT_TGZ)

AR_FILES += $(wildcard $(SD_FAT_TGZ))

SCRUB_CLEAN_FILES += $(SD_FAT_TGZ) $(SD_FAT_TGZ_STAGE_DIR)

################################################


################################################
# Clean-up and Archive

AR_FILES += $(filter-out $(AR_FILTER_OUT),$(wildcard $(AR_REGEX)))

CLEAN_FILES += $(filter-out $(AR_DIR) $(AR_FILES),$(wildcard *))

HELP_TARGETS += tgz
tgz.HELP := Create a tarball with the barebones source files that comprise this design

.PHONY: tarball tgz
tarball tgz: $(AR_FILE)

$(AR_FILE):
	@$(MKDIR) $(@D)
	@$(if $(wildcard $(@D)/*.tar.gz),$(MKDIR) $(@D)/.archive;$(MV) $(@D)/*.tar.gz $(@D)/.archive)
	@$(ECHO) "Generating $@..."
	@$(TAR) -czf $@ $(AR_FILES)

SCRUB_CLEAN_FILES += $(CLEAN_FILES)

HELP_TARGETS += scrub_clean
scrub_clean.HELP := Restore design to its barebones state

.PHONY: scrub scrub_clean
scrub scrub_clean:
	$(if $(strip $(wildcard $(SCRUB_CLEAN_FILES))),$(RM) $(wildcard $(SCRUB_CLEAN_FILES)),@$(ECHO) "You're already as clean as it gets!")

.PHONY: tgz_scrub_clean
tgz_scrub_clean:
	$(FIND) $(SOFTWARE_DIR) \( -name '*.o' -o -name '.depend*' -o -name '*.d' -o -name '*.dep' \) -delete || true
	$(MAKE) tgz AR_FILE=$(AR_FILE)
	$(MAKE) -s scrub_clean
	$(TAR) -xzf $(AR_FILE)

################################################


################################################
# Running Batch Jobs
ifneq ($(BATCH_TARGETS),)

BATCH_DIR := $(if $(TMP),$(TMP)/)batch/$(AR_TIMESTAMP)

.PHONY: $(patsubst %,batch-%,$(BATCH_TARGETS))
$(patsubst %,batch-%,$(BATCH_TARGETS)): batch-%: $(AR_FILE)
	@$(RM) $(BATCH_DIR)
	@$(MKDIR) $(BATCH_DIR)
	$(CP) $< $(BATCH_DIR) 
	$(CD) $(BATCH_DIR) && $(TAR) -xzf $(notdir $<) && $(CHMOD) -R 755 *
	$(MAKE) -C $(BATCH_DIR) $*

endif # BATCH_TARGETS != <empty>
################################################


################################################
# Help system

HELP_TARGETS += help
help.HELP := Displays this info (i.e. the available targets)

.PHONY: help
help: help-init help-targets help-fini

HELP_TARGETS_X := $(patsubst %,help-%,$(sort $(HELP_TARGETS)))
.PHONY: $(HELP_TARGETS_X)
help-targets: $(HELP_TARGETS_X)
$(HELP_TARGETS_X): help-%:
	@$(ECHO) "*********************"
	@$(ECHO) "* Target: $*"
	@$(ECHO) "*   $($*.HELP)"

.PHONY: help-init
help-init:
	@$(ECHO) "*****************************************"
	@$(ECHO) "*                                       *"
	@$(ECHO) "* Manage Quartus Prime / QSys Design    *"
	@$(ECHO) "*                                       *"
	@$(ECHO) "*     Copyright (c) 2016                *"
	@$(ECHO) "*     All Rights Reserved               *"
	@$(ECHO) "*                                       *"
	@$(ECHO) "*****************************************"
	@$(ECHO) ""

.PHONY: help-fini
help-fini:
	@$(ECHO) "*********************"

################################################
