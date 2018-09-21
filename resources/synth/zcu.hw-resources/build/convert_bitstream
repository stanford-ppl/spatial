#!/bin/bash

## Convert_bitstream: Convert a bitstream (.bit) generated using Xilinx's bitgen
## to a binary format used to program the PL through the /dev/xdevcfg file system interface.
## For details on conversion refer to: http://www.xilinx.com/support/answers/46913.html
##
## Author: Raghu Prabhakar
## Date: 01/01/2015

generate_bif_file()
{
  # Hard-coding paths for fsbl and u-boot to use the ones in this folder
  FSBL_PATH=fsbl.elf
  BITFILE_PATH=$1
  U_BOOT_PATH=u-boot.elf
  BIF_FILE=BOOT.bif
  echo "the_ROM_image:"              > ${BIF_FILE}
  echo "{"                          >> ${BIF_FILE}
  echo "  [bootloader]${FSBL_PATH}" >> ${BIF_FILE}
  echo "  ${BITFILE_PATH}"          >> ${BIF_FILE}
  echo "  ${U_BOOT_PATH}"           >> ${BIF_FILE}
  echo "}"                          >> ${BIF_FILE}
}

if [ $# -lt 2 ]; then
  echo "Usage: $0 <in_bitfile> <out_bitfile_path>"
  exit -1
fi

## Verify that the input file exists
if [ ! -f $1 ]; then
    echo "File '$1' doesn't exist, aborting"
    exit -2
fi

## Convert input paths to absolute paths, in case they are relative paths
IN_BITFILE_PATH=`readlink -f $1`
OUT_BITFILE_PATH=`readlink -f $2`

## Get directory in which this script is stored, irrespective of from where it is called and change to that
SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
pushd ${SCRIPT_DIR} > /dev/null 

## 0. Clean any stale bif and bin files lying around
rm -f *.bif *.bin *.BIN

## 1. Generate BOOT.bif file to create binary
echo "Building BOOT.bif" 
generate_bif_file ${IN_BITFILE_PATH}

## 2. Lame step 1 - Build BOOT.BIN image using Xilinx's bootgen.
echo "Building BOOT.BIN"
if [[ $3 == "ZCU102" ]]; then
  bootgen -image BOOT.bif -o i BOOT.BIN -packagename xczu9eg
else
  bootgen -image BOOT.bif -o i BOOT.BIN
fi
  
## 3. Lame step 2 - Split the BOOT.BIN to get its constituent binaries, pick system.bit.bin
##    WHY? Because Xilinx adds some magic to the .bit file in bootgen. 
##    In order to configure the PL from Linux running on ARM using /dev/xdevcfg, we need this magic bitstream.
##    http://www.xilinx.com/support/answers/46913.html
echo "Splitting BOOT.BIN" 
echo "Building BOOT.BIN"
if [[ $3 == "ZCU102" ]]; then
  bootgen -image BOOT.bif -split bin -o i BOOT.BIN -packagename xczu9eg
else
  bootgen -image BOOT.bif -split bin -o i BOOT.BIN
fi

## 4. Clean unwanted files. We only need system.bit.bin
#mv system.bit.bin out.out
#rm -f *.bin 
#mv out.out ${OUT_BITFILE_PATH}

popd > /dev/null
echo "Done"
