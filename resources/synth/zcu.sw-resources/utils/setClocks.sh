#!/bin/bash

if [ $# -lt 1 ]; then
  echo "Usage: $0 <freqMHz>"
  exit -1
fi

FCLK0_FREQ_MHZ=$1
FCLK1_FREQ_MHZ=250

setClockMHz() {
  CLK=$1
  FREQMHZ=$2
  FREQHZ=$(expr $FREQMHZ \* 1000000)

  DEVCFG_PATH=/sys/devices/soc0/amba/f8007000.devcfg
  CLK_CONTROLS_PATH=$DEVCFG_PATH/fclk/$CLK
  CLK_EXPORT_FILE=$DEVCFG_PATH/fclk_export

  grep "$CLK" $CLK_EXPORT_FILE > /dev/null
  if [ $? -eq 0 ]; then
    echo "$CLK" > $CLK_EXPORT_FILE
  fi

  echo "1" > $CLK_CONTROLS_PATH/enable
  echo "$FREQHZ" > $CLK_CONTROLS_PATH/set_rate

  SET_FREQ=$(cat $CLK_CONTROLS_PATH/set_rate)
  echo "Clock $CLK set to $SET_FREQ Hz"
}

setClockMHz fclk0 $FCLK0_FREQ_MHZ  # Design clock
setClockMHz fclk1 $FCLK1_FREQ_MHZ  # Faster clock for HP* interfaces to DDR
