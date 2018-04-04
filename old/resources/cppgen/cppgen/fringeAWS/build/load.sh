#!/bin/bash

# exit on error
set -e

AGFI_ID=""

function usage()
{
    echo "Script to assist in loading AFI onto F1 FPGA and reload driver, requires user to have sudo access"
    echo "Usage:"
    echo ""
    echo "load.sh"
    echo "        -h --help"
    echo "        -i=<agfi-id> | --agfi-id=<agfi-id>"
    echo ""
}

while [ "$1" != "" ]; do
    PARAM=`echo $1 | awk -F= '{print $1}'`
    VALUE=`echo $1 | awk -F= '{print $2}'`
    case $PARAM in
        -h | --help)
            usage
            exit
            ;;
        -i | --agfi-id)
            AGFI_ID=$VALUE
            ;;
        *)
            echo "ERROR: unknown parameter \"$PARAM\""
            usage
            exit 1
            ;;
    esac
    shift
done

echo 'Clearing FPGA image...'
sudo fpga-clear-local-image -S 0

echo 'Loading image...'
sudo fpga-load-local-image -S 0 -I $AGFI_ID

echo 'Image loaded, info:'
sudo fpga-describe-local-image -S 0 -R -H

echo 'Stopping/restarting AWS EDMA driver...'
sleep 1
sudo rmmod edma-drv && sudo insmod $AWS_HOME/sdk/linux_kernel_drivers/edma/edma-drv.ko
lsmod | grep edma

echo 'Application is now ready to run using "sudo ./Top <args>" (make sure not to forget sudo)'
