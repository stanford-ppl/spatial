# Set compiler args
CC=g++
CFLAGS=-DZYNQ -std=c++11 -Wall -c -g -O0 -ftree-vectorize
LDFLAGS=
LDLIBS=-L /usr/lib
SOURCES=Top.cpp ZynqUtils.cpp
OBJECTS=$(SOURCES:.cpp=.o)
EXECUTABLE=Top

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE):$(OBJECTS)
	$(CC) -o $@ $(LDFLAGS) $(OBJECTS) $(LDLIBS)
	utils/set_perms $(EXECUTABLE)

.cpp.o:
	$(CC) $(CFLAGS) $< -o $@

clean:
	\rm -f *.o $(EXECUTABLE) $(TAR)
