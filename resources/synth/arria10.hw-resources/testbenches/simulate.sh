rm arria10_argInOuts.vcd
iverilog -o arria10Test Top.v Arria10_tb.v RetimeShiftRegister.v SRAMVerilogAWS.v
vvp arria10Test
echo "regenerated"
