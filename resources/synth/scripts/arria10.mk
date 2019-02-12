CC=g++
LINK=g++
CROSS_COMPILE=arm-linux-gnueabihf-

FRINGE_SRC=../arria10.sw-resources
HOST_SRC=./

SOURCES := $(wildcard ${HOST_SRC}/*.cpp ${FRINGE_SRC}/*.cpp)

INCLUDES +=													\
			-I${HOST_SRC}/                \
			-I${HOST_SRC}/datastructures 	\
			-I$(JAVA_HOME)/include 				\
			-I$(JAVA_HOME)/include/linux 	\
			-I../arria10.sw-resources 					  	\
			-I${FRINGE_SRC} 					  	\


OBJECTS=$(SOURCES:.cpp=.o)

DEFINES=$(OBJECTS:.o=.d)

CXXFLAGS=-DARRIA10 -D__USE_STD_STRING__ -std=c++11 -O0 -g -marm -march=armv7-a -mcpu=cortex-a9
LDFLAGS=-Wl,--hash-style=both -lstdc++ -pthread -lpthread -lm

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

clean:
	rm -f $(OBJECTS) $(DEFINES) *.a *.vcd *.dat ${TOP} Top

.DEFAULT_GOAL := all
