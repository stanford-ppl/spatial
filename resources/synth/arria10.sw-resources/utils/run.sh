#!/bin/bash

bash set_perms Top

export PERSONA="persona1"

# Force dtbt to remove the overlay
dtbt -r ${PERSONA}.dtbo -p /boot
cp output_files/${PERSONA}.rbf /lib/firmware/
dtbt -a ${PERSONA}.dtbo -p /boot
dtbt -l

./Top $@ 2>&1 | tee run.log
