FRINGE_SRC=../SW
HOST_SRC=../
SIM_SRC=verilator_srcs

include ${FRINGE_SRC}/DUT.mk
include ${TOP}.mk

EXCLUDES= \
			${SIM_SRC}/VTop__ALLsup.cpp \
			${SIM_SRC}/VTop__ALLcls.cpp \

SOURCES := $(wildcard ${HOST_SRC}/*.cpp ${FRINGE_SRC}/*.cpp ${SIM_SRC}/*.cpp)
SOURCES := $(filter-out ${EXCLUDES}, $(SOURCES))
INCLUDES +=													\
			-I${HOST_SRC}/datastructures 	\
			-I$(JAVA_HOME)/include 				\
			-I$(JAVA_HOME)/include/linux 	\
			-I${FRINGE_SRC} 					  	\
			-I../sw-resources 					  	\
			-I${SIM_SRC}                	\

OBJECTS=$(SOURCES:.cpp=.o)

VK_GLOBAL_OBJS = $(addsuffix .o, $(VM_GLOBAL_FAST)) # See ${TOP}_classes.mk for VM_GLOBAL_FAST defn
OBJECTS+=$(VK_GLOBAL_OBJS)
DEFINES=$(OBJECTS:.o=.d)


CXXFLAGS=-DSIM -D__USE_STD_STRING__ -std=c++11 -Wno-format -Wno-unused-result
LDFLAGS=-Wl,--hash-style=both -lstdc++ -pthread -lpthread -lm

all: verilatorCrapClean pre-build-checks Top

pre-build-checks: verilatorCrapClean
ifndef JAVA_HOME
GUESS=$(shell readlink -f $(shell dirname $(shell readlink -f `which java`))/../../)
$(warning JAVA_HOME is not set, guessing to be ${GUESS}!)
JAVA_HOME=$(GUESS)
endif


Top: $(OBJECTS)
	$(LINK) $(LDFLAGS) $^ $(LOADLIBES) $(LDLIBS) -o $@ $(LIBS) $(SC_LIBS) 2>&1 | c++filt

%.o: %.cpp
	  time $(CXX) $(INCLUDES) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@  $<

## Clean up
verilatorCrapClean:
	rm -f ${SIM_SRC}/VTop__ALLcls.cpp ${SIM_SRC}/VTop__ALLsup.cpp

clean: verilatorCrapClean
	rm -f $(OBJECTS) $(DEFINES) *.a *.vcd *.dat ${TOP} Top

# Set the default Makefile goal to be 'all', else it will default to executing
# the first target in ${TOP}.mk
.DEFAULT_GOAL := all
