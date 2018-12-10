#!/bin/bash

bash set_perms Top

# Set CLOCK_FREQ_MHZ variable as follows:
# 1. Honor commandline override if CLOCK_FREQ_MHZ is defined in the calling environment.
# 2. If CLOCK_FREQ_MHZ is unset, source the 'parClockFreq.sh' script generated during place-and-route
#    This script sets CLOCK_FREQ_MHZ to the target clock frequency used for place and route
if [ -z "$CLOCK_FREQ_MHZ" ]; then
  source parClockFreq.sh
  if [ -z "$CLOCK_FREQ_MHZ" ]; then
    echo "CLOCK_FREQ_MHZ variable is unset; set it to the desired clock freq";
    exit -1
  fi
fi

# Set FCLKCLK0 (fabric clock) to clock frequency in CLOCK_FREQ_MHZ
sudo su -c "bash setClocks.sh $CLOCK_FREQ_MHZ"

./Top $@ 2>&1 | tee run.log
