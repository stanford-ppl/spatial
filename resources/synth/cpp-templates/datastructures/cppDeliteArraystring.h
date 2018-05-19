#ifndef __cppDeliteArraystring__
#define __cppDeliteArraystring__

#include "DeliteNamespaces.h"
#include "DeliteMemory.h"
#ifdef __DELITE_CPP_NUMA__
#include <numa.h>
#endif

class cppDeliteArraystring : public DeliteMemory {
public:
  string  *data;
  int length;

  cppDeliteArraystring(int _length, resourceInfo_t *resourceInfo): data((string  *)(new (resourceInfo) string [_length])), length(_length) { }

  cppDeliteArraystring(int _length): data((string  *)(new string [_length])), length(_length) { }

  cppDeliteArraystring(string  *_data, int _length) {
    data = _data;
    length = _length;
  }

  string  apply(int idx) {
    return data[idx];
  }

  void update(int idx, string  val) {
    data[idx] = val;
  }

  void print(void) {
    printf("length is %d\n", length);
  }

  bool equals(cppDeliteArraystring *to) {
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

struct cppDeliteArraystringD {
  void operator()(cppDeliteArraystring *p) {
    //printf("cppDeliteArraystring: deleting %p\n",p);
    delete[] p->data;
  }

/*
#ifdef __DELITE_CPP_NUMA__
  const bool isNuma;
  string  **wrapper;
  size_t numGhostCells; // constant for all internal arrays
  size_t *starts;
  size_t *ends;
  size_t numChunks;

  cppDeliteArraystring(int _length, resourceInfo_t *resourceInfo): data((string  *)(new (resourceInfo) string [_length])), length(_length), isNuma(false) { }

  cppDeliteArraystring(int _length): data((string  *)(new string [_length])), length(_length), isNuma(false) { }

  cppDeliteArraystring(string  *_data, size_t _length): data(_data), length(_length), isNuma(false) { }

  cppDeliteArraystring(size_t _length, size_t _numGhostCells, size_t _numChunks) : data(NULL), length(_length), isNuma(true) {
    //FIXME: transfer functions rely on data field
    numGhostCells = _numGhostCells;
    numChunks = _numChunks;
    wrapper = (string  **)malloc(numChunks*sizeof(string *));
    starts = (size_t *)malloc(numChunks*sizeof(size_t)); //TODO: custom partitioning
    ends = (size_t *)malloc(numChunks*sizeof(size_t));
    for (int sid = 0; sid < numChunks; sid++) {
      starts[sid] = std::max(length * sid / numChunks - numGhostCells, (size_t)0);
      ends[sid] = std::min(length * (sid+1) / numChunks + numGhostCells, length);
      allocInternal(sid, ends[sid]-starts[sid]);
    }
  }

  void allocInternal(int socketId, size_t length) {
    wrapper[socketId] = (string *)numa_alloc_onnode(length*sizeof(string ), socketId);
  }

  string  apply(size_t idx) {
    if (isNuma) {
      for (size_t sid = 0; sid < numChunks; sid++) {
        if (idx < ends[sid]) return wrapper[sid][idx-starts[sid]]; //read from first location found
      }
      assert(false); //throw runtime_exception
    }
    else
      return data[idx];
  }

  void update(size_t idx, string  val) {
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
  string  applyAt(size_t idx, size_t sid) {
    //size_t sid = config->threadToSocket(tid);
    size_t offset = starts[sid];
    if (idx >= offset && idx < ends[sid]) return wrapper[sid][idx-offset];
    return apply(idx);
  }

  string  unsafe_apply(size_t socketId, size_t idx) {
    return wrapper[socketId][idx];
  }

  //update locally, ghosts need to be explicitly synchronized
  void updateAt(size_t idx, string  value, size_t sid) {
    //size_t sid = config->threadToSocket(tid);
    size_t offset = starts[sid];
    if (idx >= offset && idx < ends[sid]) wrapper[sid][idx-offset] = value;
    //else throw runtime_exception
  }

  void unsafe_update(size_t socketId, size_t idx, string  value) {
    wrapper[socketId][idx] = value;
  }
#endif
*/
};

#endif
