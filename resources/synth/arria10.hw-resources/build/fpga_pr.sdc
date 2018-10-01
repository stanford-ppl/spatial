# False path to the PIO instances for acknowledgement delay testing only
create_clock -name frz_ack_pio -period 10.000 [get_registers {soc_inst|frz_ack_pio|frz_ack_pio|data_out[0]}]
set_false_path -from [get_clocks {frz_ack_pio}] -to *
set_false_path -from * -to [get_clocks {frz_ack_pio}]

create_clock -name stop_ack_pio -period 10.000 [get_registers {soc_inst|stop_ack_pio|stop_ack_pio|data_out[0]}]
set_false_path -from [get_clocks {stop_ack_pio}] -to *
set_false_path -from * -to [get_clocks {stop_ack_pio}]

create_clock -name start_ack_pio -period 10.000 [get_registers {soc_inst|start_ack_pio|start_ack_pio|data_out[0]}]
set_false_path -from [get_clocks {start_ack_pio}] -to *
set_false_path -from * -to [get_clocks {start_ack_pio}]