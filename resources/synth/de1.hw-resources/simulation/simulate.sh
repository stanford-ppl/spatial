cp ../Top.v ./
echo "refreshed Top.v"
rm test.vcd
iverilog -o topTest Top.v Top_tb.v
vvp topTest
echo "regenerated"
