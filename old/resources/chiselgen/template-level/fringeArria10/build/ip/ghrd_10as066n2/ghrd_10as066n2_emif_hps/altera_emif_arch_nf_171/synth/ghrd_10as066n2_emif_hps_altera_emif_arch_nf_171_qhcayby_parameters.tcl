# (C) 2001-2017 Intel Corporation. All rights reserved.
# Your use of Intel Corporation's design tools, logic functions and other 
# software and tools, and its AMPP partner logic functions, and any output 
# files from any of the foregoing (including device programming or simulation 
# files), and any associated documentation or information are expressly subject 
# to the terms and conditions of the Intel Program License Subscription 
# Agreement, Intel FPGA IP License Agreement, or other applicable 
# license agreement, including, without limitation, that your use is for the 
# sole purpose of programming logic devices manufactured by Intel and sold by 
# Intel or its authorized distributors.  Please refer to the applicable 
# agreement for further details.


#####################################################################
#
# THIS IS AN AUTO-GENERATED FILE!
# -------------------------------
# If you modify this files, all your changes will be lost if you
# regenerate the core!
#
# FILE DESCRIPTION
# ----------------
# This file specifies the timing properties of the memory device and
# of the memory interface


set var(VIN_Cs) [expr [emiftcl_get_parameter_value -parameter VIN_Cs]/1000.0]
set var(VIN_Ch) [expr [emiftcl_get_parameter_value -parameter VIN_Ch]/1000.0]
set var(MPR_DQSQ) [emiftcl_get_parameter_value -parameter MPR_DQSQ]
set var(MPR_QH) [emiftcl_get_parameter_value -parameter MPR_QH]
set var(MPR_DS) [emiftcl_get_parameter_value -parameter MPR_DS]
set var(MPR_DH) [emiftcl_get_parameter_value -parameter MPR_DH]
set var(MPR_IS) [emiftcl_get_parameter_value -parameter MPR_IS]
set var(MPR_IH) [emiftcl_get_parameter_value -parameter MPR_IH]
set var(MPR_DQSCK) [emiftcl_get_parameter_value -parameter MPR_DQSCK]
set var(MPR_DQSS) [emiftcl_get_parameter_value -parameter MPR_DQSS]
set var(MPR_WLS) [emiftcl_get_parameter_value -parameter MPR_WLS]
set var(MPR_WLH) [emiftcl_get_parameter_value -parameter MPR_WLH]
set var(MPR_DSS) [emiftcl_get_parameter_value -parameter MPR_DSS]
set var(MPR_DSH) [emiftcl_get_parameter_value -parameter MPR_DSH]
set var(WITH_MPR) [emiftcl_get_parameter_value -parameter WITH_MPR]
set var(WITH_WL_M_CALIB) [emiftcl_get_parameter_value -parameter WITH_WL_M_CALIB]
set var(WITH_CA_CALIB) [emiftcl_get_parameter_value -parameter WITH_CA_CALIB]
set var(WITH_WL_CALIB) [emiftcl_get_parameter_value -parameter WITH_WL_CALIB]
set var(DEFAULT_BD_PKG_SKEW) [expr [emiftcl_get_parameter_value -parameter DEFAULT_BD_PKG_SKEW]/1000.0]
set var(DEFAULT_CA_BD_PKG_SKEW) [expr [emiftcl_get_parameter_value -parameter DEFAULT_CA_BD_PKG_SKEW]/1000.0]
set var(DEFAULT_CA_TO_CK_BD_PKG_SKEW) [expr [emiftcl_get_parameter_value -parameter DEFAULT_CA_TO_CK_BD_PKG_SKEW]/1000.0]
set var(BD_SK_SENS_RD) [emiftcl_get_parameter_value -parameter BD_SK_SENS_RD]
set var(BD_SK_SENS_WR) [emiftcl_get_parameter_value -parameter BD_SK_SENS_WR]
set var(BD_SK_SENS_DQSG) [emiftcl_get_parameter_value -parameter BD_SK_SENS_DQSG]
set var(BD_SK_SENS_WL) [emiftcl_get_parameter_value -parameter BD_SK_SENS_WL]
set var(BD_SK_SENS_WL_WO_CALIB) [emiftcl_get_parameter_value -parameter BD_SK_SENS_WL_WO_CALIB]
set var(BD_SK_SENS_CA) [emiftcl_get_parameter_value -parameter BD_SK_SENS_CA]
set var(BD_SK_SENS_CA_WO_CALIB) [emiftcl_get_parameter_value -parameter BD_SK_SENS_CA_WO_CALIB]
set var(EXTRACTED_PERIOD) [emiftcl_get_parameter_value -parameter EXTRACTED_PERIOD]
set var(RD_JITTER_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter RD_JITTER_SENS_TO_PERIOD]
set var(RD_CALUNC_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter RD_CALUNC_SENS_TO_PERIOD]
set var(WR_JITTER_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter WR_JITTER_SENS_TO_PERIOD]
set var(WR_CALUNC_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter WR_CALUNC_SENS_TO_PERIOD]
set var(CA_JITTER_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter CA_JITTER_SENS_TO_PERIOD]
set var(CA_CALUNC_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter CA_CALUNC_SENS_TO_PERIOD]
set var(CA_PVT_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter CA_PVT_SENS_TO_PERIOD]
set var(DQSG_JITTER_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter DQSG_JITTER_SENS_TO_PERIOD]
set var(DQSG_TRKUNC_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter DQSG_TRKUNC_SENS_TO_PERIOD]
set var(WL_JITTER_SENS_TO_PERIOD) [emiftcl_get_parameter_value -parameter WL_JITTER_SENS_TO_PERIOD]
set var(RD_TEMP_CAL_LOSS_OCT_RECAL) [expr [emiftcl_get_parameter_value -parameter RD_TEMP_CAL_LOSS_OCT_RECAL]/1000.0]
set var(RD_TEMP_CAL_LOSS_WO_OCT_RECAL) [expr [emiftcl_get_parameter_value -parameter RD_TEMP_CAL_LOSS_WO_OCT_RECAL]/1000.0]
set var(RD_CALIBRATION_LOSS_OTHER) [expr [emiftcl_get_parameter_value -parameter RD_CALIBRATION_LOSS_OTHER]/1000.0]
set var(RD_DBI_EFFECT) [expr [emiftcl_get_parameter_value -parameter RD_DBI_EFFECT]/1000.0]
set var(RD_TEMP_CAL_LOSS_WO_DLL) [expr [emiftcl_get_parameter_value -parameter RD_TEMP_CAL_LOSS_WO_DLL]/1000.0]
set var(WR_TEMP_CAL_LOSS_OCT_RECAL) [expr [emiftcl_get_parameter_value -parameter WR_TEMP_CAL_LOSS_OCT_RECAL]/1000.0]
set var(WR_TEMP_CAL_LOSS_WO_OCT_RECAL) [expr [emiftcl_get_parameter_value -parameter WR_TEMP_CAL_LOSS_WO_OCT_RECAL]/1000.0]
set var(WR_CALIBRATION_LOSS_OTHER) [expr [emiftcl_get_parameter_value -parameter WR_CALIBRATION_LOSS_OTHER]/1000.0]
set var(CA_TEMP_CAL_LOSS_OCT_RECAL_CA_CAL) [expr [emiftcl_get_parameter_value -parameter CA_TEMP_CAL_LOSS_OCT_RECAL_CA_CAL]/1000.0]
set var(CA_TEMP_CAL_LOSS_WO_OCT_RECAL_CA_CAL) [expr [emiftcl_get_parameter_value -parameter CA_TEMP_CAL_LOSS_WO_OCT_RECAL_CA_CAL]/1000.0]
set var(CA_TEMP_CAL_LOSS_OCT_RECAL_WO_CA_CAL) [expr [emiftcl_get_parameter_value -parameter CA_TEMP_CAL_LOSS_OCT_RECAL_WO_CA_CAL]/1000.0]
set var(CA_TEMP_CAL_LOSS_WO_OCT_RECAL_WO_CA_CAL) [expr [emiftcl_get_parameter_value -parameter CA_TEMP_CAL_LOSS_WO_OCT_RECAL_WO_CA_CAL]/1000.0]
set var(CA_CALIBRATION_LOSS_OTHER) [expr [emiftcl_get_parameter_value -parameter CA_CALIBRATION_LOSS_OTHER]/1000.0]
set var(WL_TEMP_CAL_LOSS_OCT_RECAL) [expr [emiftcl_get_parameter_value -parameter WL_TEMP_CAL_LOSS_OCT_RECAL]/1000.0]
set var(WL_TEMP_CAL_LOSS_WO_OCT_RECAL) [expr [emiftcl_get_parameter_value -parameter WL_TEMP_CAL_LOSS_WO_OCT_RECAL]/1000.0]
set var(WL_TEMP_CAL_LOSS_OCT_RECAL_WO_WL_CAL) [expr [emiftcl_get_parameter_value -parameter WL_TEMP_CAL_LOSS_OCT_RECAL_WO_WL_CAL]/1000.0]
set var(WL_TEMP_CAL_LOSS_WO_OCT_RECAL_WO_WL_CAL) [expr [emiftcl_get_parameter_value -parameter WL_TEMP_CAL_LOSS_WO_OCT_RECAL_WO_WL_CAL]/1000.0]
set var(WL_CALIBRATION_LOSS_OTHER) [expr [emiftcl_get_parameter_value -parameter WL_CALIBRATION_LOSS_OTHER]/1000.0]
set var(RD_DBI_EFFECT) [expr [emiftcl_get_parameter_value -parameter RD_DBI_EFFECT]/1000.0]
set var(TERMINATION_LOSS_DEVICE_60) [expr [emiftcl_get_parameter_value -parameter TERMINATION_LOSS_DEVICE_60]/1000.0]
set var(TERMINATION_LOSS_DIMM_60) [expr [emiftcl_get_parameter_value -parameter TERMINATION_LOSS_DIMM_60]/1000.0]
set var(TERMINATION_LOSS_DEVICE_120) [expr [emiftcl_get_parameter_value -parameter TERMINATION_LOSS_DEVICE_120]/1000.0]
set var(TERMINATION_LOSS_DIMM_120) [expr [emiftcl_get_parameter_value -parameter TERMINATION_LOSS_DIMM_120]/1000.0]
set var(RD_SSI) [expr [emiftcl_get_parameter_value -parameter RD_SSI]/1000.0]
set var(RD_JITTER) [expr [emiftcl_get_parameter_value -parameter RD_JITTER]/1000.0]
set var(RD_DCD) [expr [emiftcl_get_parameter_value -parameter RD_DCD]/1000.0]
set var(RD_SH) [expr [emiftcl_get_parameter_value -parameter RD_SH]/1000.0]
set var(RD_EOL) [expr [emiftcl_get_parameter_value -parameter RD_EOL]/1000.0]
set var(WR_SSO) [expr [emiftcl_get_parameter_value -parameter WR_SSO]/1000.0]
set var(WR_JITTER) [expr [emiftcl_get_parameter_value -parameter WR_JITTER]/1000.0]
set var(WR_DCD) [expr [emiftcl_get_parameter_value -parameter WR_DCD]/1000.0]
set var(WR_EOL) [expr [emiftcl_get_parameter_value -parameter WR_EOL]/1000.0]
set var(CA_SSO) [expr [emiftcl_get_parameter_value -parameter CA_SSO]/1000.0]
set var(CA_JITTER) [expr [emiftcl_get_parameter_value -parameter CA_JITTER]/1000.0]
set var(CA_DCD) [expr [emiftcl_get_parameter_value -parameter CA_DCD]/1000.0]
set var(CA_EOL) [expr [emiftcl_get_parameter_value -parameter CA_EOL]/1000.0]
set var(DQSG_SSI) [expr [emiftcl_get_parameter_value -parameter DQSG_SSI]/1000.0]
set var(DQSG_JITTER) [expr [emiftcl_get_parameter_value -parameter DQSG_JITTER]/1000.0]
set var(DQSG_DCD) [expr [emiftcl_get_parameter_value -parameter DQSG_DCD]/1000.0]
set var(DQSG_EOL) [expr [emiftcl_get_parameter_value -parameter DQSG_EOL]/1000.0]
set var(DQSG_CAL_UNC) [expr [emiftcl_get_parameter_value -parameter DQSG_CAL_UNC]/1000.0]
set var(DQSG_TRK_UNC) [expr [emiftcl_get_parameter_value -parameter DQSG_TRK_UNC]/1000.0]
set var(DQSG_SH) [expr [emiftcl_get_parameter_value -parameter DQSG_SH]/1000.0]
set var(WL_SSO) [expr [emiftcl_get_parameter_value -parameter WL_SSO]/1000.0]
set var(WL_JITTER) [expr [emiftcl_get_parameter_value -parameter WL_JITTER]/1000.0]
set var(WL_DCD) [expr [emiftcl_get_parameter_value -parameter WL_DCD]/1000.0]
set var(WL_EOL) [expr [emiftcl_get_parameter_value -parameter WL_EOL]/1000.0]
