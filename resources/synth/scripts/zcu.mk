CC=g++
LINK=g++
CROSS_COMPILE=/usr/bin/aarch64-linux-gnu-

FRINGE_SRC=../zcu.sw-resources
HOST_SRC=./

SOURCES := $(wildcard ${HOST_SRC}/*.cpp ${FRINGE_SRC}/*.cpp)

INCLUDES +=													\
			-I${HOST_SRC}/                \
			-I${HOST_SRC}/datastructures 	\
			-I$(JAVA_HOME)/include 				\
			-I$(JAVA_HOME)/include/linux 	\
			-I${FRINGE_SRC} 					  	\
			-I${HOST_SRC}/SW/xil_libs					\


OBJECTS=$(SOURCES:.cpp=.o)

DEFINES=$(OBJECTS:.o=.d)

CXXFLAGS=-DZCU -D__USE_STD_STRING__  -D_GLIBCXX_USE_CXX11_ABI=0 -std=c++11 -O0 -g -march=armv8-a -mcpu=cortex-a53
LDFLAGS=-Wl,--hash-style=both -lstdc++ -pthread -lpthread -lm # -L${HOST_SRC}/SW/xil_libs -lxil

all: pre-build-checks Top

pre-build-checks:
ifndef JAVA_HOME
GUESS=$(shell readlink -f $(shell dirname $(shell readlink -f `which java`))/../../)
$(warning JAVA_HOME is not set, guessing to be ${GUESS}!)
JAVA_HOME=$(GUESS)
endif


Top: $(OBJECTS)
	$(CROSS_COMPILE)$(LINK) $^ $(LOADLIBES) $(LDLIBS) -o $@ $(LIBS) $(SC_LIBS) $(LDFLAGS) 2>&1 | c++filt

%.o: %.cpp
	  $(CROSS_COMPILE)$(CC) $(INCLUDES) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@  $<

## Clean up
clean:
	rm -f $(OBJECTS) $(DEFINES) *.a *.vcd *.dat ${TOP} Top *.tar.gz
#	rm -f generated_*

# Set the default Makefile goal to be 'all', else it will default to executing
# the first target in ${TOP}.mk
.DEFAULT_GOAL := all
