# TODO:
# See comments in FringeContextAWS.h -- eventually this script will be done within fringe load()
# This script can also parse args and call sudo ./Top directly as its final step

# Note: in older version of the SDK (prior to 1.3.0), running this command was sometimes needed
# to stop/restart the EDMA driver
# sudo rmmod edma-drv && sudo insmod /home/centos/src/project_data/aws-fpga/sdk/linux_kernel_drivers/edma/edma-drv.ko
#
# Now, to remove and re-add the xdma driver and reset the device file mappings, run:
# sudo rmmod xdma && sudo insmod /home/centos/src/project_data/aws-fpga/sdk/linux_kernel_drivers/xdma/xdma.ko

# Note: to clear the image, use:    sudo fpga-clear-local-image -S 0

echo 'Loading image...'
sudo fpga-load-local-image -S 0 -I agfi-[PLACE ID HERE]
# sudo fpga-describe-local-image -S 0 -R -H
echo 'Application is now ready to run using "sudo ./Top <args>" (make sure not to forget sudo)'
