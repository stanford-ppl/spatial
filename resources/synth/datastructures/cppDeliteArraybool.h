#ifndef __cppDeliteArraybool__
#define __cppDeliteArraybool__

#include "DeliteNamespaces.h"
#include "DeliteMemory.h"
#ifdef __DELITE_CPP_NUMA__
#include <numa.h>
#endif

class cppDeliteArraybool : public DeliteMemory {
public:
  bool  *data;
  int length;

  cppDeliteArraybool(int _length, resourceInfo_t *resourceInfo): data((bool  *)(new (resourceInfo) bool [_length])), length(_length) { }

  cppDeliteArraybool(int _length): data((bool  *)(new bool [_length])), length(_length) { }

  cppDeliteArraybool(bool  *_data, int _length) {
    data = _data;
    length = _length;
  }

  bool  apply(int idx) {
    return data[idx];
  }

  void update(int idx, bool  val) {
    data[idx] = val;
  }

  void print(void) {
    printf("length is %d\n", length);
  }

  bool equals(cppDeliteArraybool *to) {
    return this == this;
  }

  uint32_t hashcode(void) {
    return (uintptr_t)this;
  }

#ifdef DELITE_GC
  void deepCopy(void) {
  }
#endif

};

struct cppDeliteArrayboolD {
  void operator()(cppDeliteArraybool *p) {
    //printf("cppDeliteArraybool: deleting %p\n",p);
    delete[] p->data;
  }

/*
#ifdef __DELITE_CPP_NUMA__
  const bool isNuma;
  bool  **wrapper;
  size_t numGhostCells; // constant for all internal arrays
  size_t *starts;
  size_t *ends;
  size_t numChunks;

  cppDeliteArraybool(int _length, resourceInfo_t *resourceInfo): data((bool  *)(new (resourceInfo) bool [_length])), length(_length), isNuma(false) { }

  cppDeliteArraybool(int _length): data((bool  *)(new bool [_length])), length(_length), isNuma(false) { }

  cppDeliteArraybool(bool  *_data, size_t _length): data(_data), length(_length), isNuma(false) { }

  cppDeliteArraybool(size_t _length, size_t _numGhostCells, size_t _numChunks) : data(NULL), length(_length), isNuma(true) {
    //FIXME: transfer functions rely on data field
    numGhostCells = _numGhostCells;
    numChunks = _numChunks;
    wrapper = (bool  **)malloc(numChunks*sizeof(bool *));
    starts = (size_t *)malloc(numChunks*sizeof(size_t)); //TODO: custom partitioning
    ends = (size_t *)malloc(numChunks*sizeof(size_t));
    for (int sid = 0; sid < numChunks; sid++) {
      starts[sid] = std::max(length * sid / numChunks - numGhostCells, (size_t)0);
      ends[sid] = std::min(length * (sid+1) / numChunks + numGhostCells, length);
      allocInternal(sid, ends[sid]-starts[sid]);
    }
  }

  void allocInternal(int socketId, size_t length) {
    wrapper[socketId] = (bool *)numa_alloc_onnode(length*sizeof(bool ), socketId);
  }

  bool  apply(size_t idx) {
    if (isNuma) {
      for (size_t sid = 0; sid < numChunks; sid++) {
        if (idx < ends[sid]) return wrapper[sid][idx-starts[sid]]; //read from first location found
      }
      assert(false); //throw runtime_exception
    }
    else
      return data[idx];
  }

  void update(size_t idx, bool  val) {
    if (isNuma) {
      for (size_t sid = 0; sid < numChunks; sid++) {
        size_t offset = starts[sid];
        if (idx >= offset && idx < ends[sid]) wrapper[sid][idx-offset] = val; //update all ghosts
      }
    }
    else
      data[idx] = val;
  }

  //read locally if available, else remotely
  bool  applyAt(size_t idx, size_t sid) {
    //size_t sid = config->threadToSocket(tid);
    size_t offset = starts[sid];
    if (idx >= offset && idx < ends[sid]) return wrapper[sid][idx-offset];
    return apply(idx);
  }

  bool  unsafe_apply(size_t socketId, size_t idx) {
    return wrapper[socketId][idx];
  }

  //update locally, ghosts need to be explicitly synchronized
  void updateAt(size_t idx, bool  value, size_t sid) {
    //size_t sid = config->threadToSocket(tid);
    size_t offset = starts[sid];
    if (idx >= offset && idx < ends[sid]) wrapper[sid][idx-offset] = value;
    //else throw runtime_exception
  }

  void unsafe_update(size_t socketId, size_t idx, bool  value) {
    wrapper[socketId][idx] = value;
  }
#endif
*/
};

#endif
