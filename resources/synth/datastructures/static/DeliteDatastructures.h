#ifndef __DELITE_DATASTRUCTURES_H__
#define __DELITE_DATASTRUCTURES_H__

#include "DeliteCppRandom.h"

// structure to keep thread-local resource information
typedef struct resourceInfo_t {
  int threadId;
  int numThreads;
  int socketId;
  int numSockets;
  //int slaveId;
  //int numSlaves;
  int groupId;
  int groupSize;
  int availableThreads;
  //TODO: move thread-local rand to somewhere
  DeliteCppRandom *rand;

  resourceInfo_t() { }

  resourceInfo_t(int _threadId, int _numThreads, int _socketId, int _numSockets) {
    threadId = _threadId; numThreads = _numThreads; socketId = _socketId; numSockets = _numSockets;
    groupId = -1; groupSize = -1; availableThreads = _numThreads;
    rand = NULL;
  }

} resourceInfo_t;

#endif
