#ifndef __DELITE_MEMORY_H__
#define __DELITE_MEMORY_H__

#include <memory>
#include <list>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#include <sched.h>
#include "DeliteDatastructures.h"

//#define DHEAP_DEBUG(...) fprintf(stderr, "[DEBUG-DeliteHeap] "); fprintf(stderr, __VA_ARGS__)
#define DHEAP_DEBUG(...)

// Delite Custom Memory APIs
void DeliteHeapInit(int numThreads, size_t heapSize);
void DeliteHeapClear(int numThreads);
char *DeliteHeapAlloc(size_t sz, int idx, bool initialize);
void DeliteHeapMark(int idx);
void DeliteHeapReset(int idx);
void DeliteHeapReset2(int idx);
void delite_barrier(unsigned int count);

// globally overloaded new operators
void *operator new(size_t sz, const resourceInfo_t *resourceInfo);
void *operator new[](size_t sz, const resourceInfo_t *resourceInfo);

class DeliteMemory {
public:
  void* operator new(size_t sz) {
    DHEAP_DEBUG("Non-local allocation with size %d\n", sz);
    return malloc(sz);
  }
  
  void* operator new(size_t sz, const resourceInfo_t *resourceInfo) {
    DHEAP_DEBUG("Allocation from thread %d with size %d\n", resourceInfo->threadId, sz);
    return DeliteHeapAlloc(sz, resourceInfo->threadId, true);
  }

  /*
  void operator delete(void*) {
  }
  */
};

#endif
