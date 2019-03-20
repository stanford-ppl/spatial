# Amazon FPGA Hardware Development Kit
#
# Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Amazon Software License (the "License"). You may not use
# this file except in compliance with the License. A copy of the License is
# located at
#
#    http://aws.amazon.com/asl/
#
# or in the "license" file accompanying this file. This file is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or
# implied. See the License for the specific language governing permissions and
# limitations under the License.


INCLUDES = -I$(SDK_DIR)/userspace/include -I ../include -I ./datastructures

CC = g++
CFLAGS = -DCONFIG_LOGLEVEL=4 -D__USE_STD_STRING__ -std=c++11 -g -Wall $(INCLUDES)

LDLIBS = -lfpga_mgmt -lrt -lpthread

SRC = TopHost.cpp
OBJ = $(SRC:.c=.o)
BIN = Top
OTHER_SRCS = $(wildcard datastructures/*.cpp) 

all: check_env $(BIN)

$(BIN): $(OBJ)
	$(CC) $(CFLAGS) $(OTHER_SRCS) -o $@ $^ $(LDFLAGS) $(LDLIBS)

clean:
	rm -f *.o $(BIN)

check_env:
ifndef SDK_DIR
    $(error SDK_DIR is undefined. Try "source sdk_setup.sh" to set the software environment)
endif
