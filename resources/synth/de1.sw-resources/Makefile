CC=g++
LINK=g++
CROSS_COMPILE=arm-linux-gnueabihf-
# arm-xilinx-linux-gnueabi- 
FRINGE_SRC=./SW
HOST_SRC=./

SOURCES := $(wildcard ${HOST_SRC}/*.cpp ${FRINGE_SRC}/*.cpp)

INCLUDES +=													\
			-I${HOST_SRC}/datastructures 	\
			-I$(JAVA_HOME)/include 				\
			-I$(JAVA_HOME)/include/linux 	\
			-I../sw-resources 					  	\
			-I${FRINGE_SRC} 					  	\


OBJECTS=$(SOURCES:.cpp=.o)

DEFINES=$(OBJECTS:.o=.d)

CXXFLAGS=-DDE1SoC  -D__USE_STD_STRING__ -std=c++11
LDFLAGS=-Wl,--hash-style=both -lstdc++ -pthread -lpthread -lm -static

all: pre-build-checks Top

pre-build-checks:
ifndef JAVA_HOME
GUESS=$(shell readlink -f $(shell dirname $(shell readlink -f `which java`))/../../)
$(warning JAVA_HOME is not set, guessing to be ${GUESS}!)
JAVA_HOME=$(GUESS)
endif


Top: $(OBJECTS)
	$(CROSS_COMPILE)$(LINK) $(LDFLAGS) $^ $(LOADLIBES) $(LDLIBS) -o $@ $(LIBS) $(SC_LIBS) 2>&1 | c++filt

%.o: %.cpp
	  $(CROSS_COMPILE)$(CC) $(INCLUDES) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@  $<

## Clean up
clean:
	rm -f $(OBJECTS) $(DEFINES) *.a *.vcd *.dat ${TOP} Top

# Set the default Makefile goal to be 'all', else it will default to executing
# the first target in ${TOP}.mk
.DEFAULT_GOAL := all
