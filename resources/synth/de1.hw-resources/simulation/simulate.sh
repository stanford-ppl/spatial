cp ../SpatialIP.v ./
echo "refreshed SpatialIP.v"
rm test.vcd
iverilog -o topTest SpatialIP.v Top_tb.v
vvp topTest
echo "regenerated"
