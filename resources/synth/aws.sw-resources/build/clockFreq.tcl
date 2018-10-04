
set CLOCK_FREQ_HZ [expr 1000000000 / $clk_main_a0_period]
set CLOCK_FREQ_MHZ [expr $CLOCK_FREQ_HZ / 1000000]

puts "CLOCK FREQUENCY for Spatial IP blocks: $CLOCK_FREQ_MHZ MHz"
