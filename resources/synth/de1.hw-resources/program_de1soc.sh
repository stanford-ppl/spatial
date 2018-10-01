echo "Disabling fpga2hps bridge..."
echo 0 > /sys/class/fpga-bridge/fpga2hps/enable
echo "Disabling hps2fpga bridge..."
echo 0 > /sys/class/fpga-bridge/hps2fpga/enable
echo "Disabling lwhps2fpga bridge..."
echo 0 > /sys/class/fpga-bridge/lwhps2fpga/enable

echo "Loading $1 into FPGA device..."
# dd if=avalon_reginit.rbf of=/dev/fpga0 bs=1M
dd if=$1 of=/dev/fpga0 bs=1M

echo "Enabling fpga2hps bridge..."
echo 1 > /sys/class/fpga-bridge/fpga2hps/enable
echo "Enabling hps2fpga bridge..."
echo 1 > /sys/class/fpga-bridge/hps2fpga/enable
echo "Enabling lwhps2fpga bridge..."
echo 1 > /sys/class/fpga-bridge/lwhps2fpga/enable
