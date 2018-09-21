#ifndef __cppDeliteArraydouble__
#define __cppDeliteArraydouble__

#include "DeliteNamespaces.h"
#include "DeliteMemory.h"
#ifdef __DELITE_CPP_NUMA__
#include <numa.h>
#endif

class cppDeliteArraydouble : public DeliteMemory {
public:
  double  *data;
  int length;

  cppDeliteArraydouble(int _length, resourceInfo_t *resourceInfo): data((double  *)(new (resourceInfo) double [_length])), length(_length) { }

  cppDeliteArraydouble(int _length): data((double  *)(new double [_length])), length(_length) { }

  cppDeliteArraydouble(double  *_data, int _length) {
    data = _data;
    length = _length;
  }

  double  apply(int idx) {
    return data[idx];
  }

  void update(int idx, double  val) {
    data[idx] = val;
  }

  void print(void) {
    printf("length is %d\n", length);
  }

  bool equals(cppDeliteArraydouble *to) {
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

struct cppDeliteArraydoubleD {
  void operator()(cppDeliteArraydouble *p) {
    //printf("cppDeliteArraydouble: deleting %p\n",p);
    delete[] p->data;
  }

/*
#ifdef __DELITE_CPP_NUMA__
  const bool isNuma;
  double  **wrapper;
  size_t numGhostCells; // constant for all internal arrays
  size_t *starts;
  size_t *ends;
  size_t numChunks;

  cppDeliteArraydouble(int _length, resourceInfo_t *resourceInfo): data((double  *)(new (resourceInfo) double [_length])), length(_length), isNuma(false) { }

  cppDeliteArraydouble(int _length): data((double  *)(new double [_length])), length(_length), isNuma(false) { }

  cppDeliteArraydouble(double  *_data, size_t _length): data(_data), length(_length), isNuma(false) { }

  cppDeliteArraydouble(size_t _length, size_t _numGhostCells, size_t _numChunks) : data(NULL), length(_length), isNuma(true) {
    //FIXME: transfer functions rely on data field
    numGhostCells = _numGhostCells;
    numChunks = _numChunks;
    wrapper = (double  **)malloc(numChunks*sizeof(double *));
    starts = (size_t *)malloc(numChunks*sizeof(size_t)); //TODO: custom partitioning
    ends = (size_t *)malloc(numChunks*sizeof(size_t));
    for (int sid = 0; sid < numChunks; sid++) {
      starts[sid] = std::max(length * sid / numChunks - numGhostCells, (size_t)0);
      ends[sid] = std::min(length * (sid+1) / numChunks + numGhostCells, length);
      allocInternal(sid, ends[sid]-starts[sid]);
    }
  }

  void allocInternal(int socketId, size_t length) {
    wrapper[socketId] = (double *)numa_alloc_onnode(length*sizeof(double ), socketId);
  }

  double  apply(size_t idx) {
    if (isNuma) {
      for (size_t sid = 0; sid < numChunks; sid++) {
        if (idx < ends[sid]) return wrapper[sid][idx-starts[sid]]; //read from first location found
      }
      assert(false); //throw runtime_exception
    }
    else
      return data[idx];
  }

  void update(size_t idx, double  val) {
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
  double  applyAt(size_t idx, size_t sid) {
    //size_t sid = config->threadToSocket(tid);
    size_t offset = starts[sid];
    if (idx >= offset && idx < ends[sid]) return wrapper[sid][idx-offset];
    return apply(idx);
  }

  double  unsafe_apply(size_t socketId, size_t idx) {
    return wrapper[socketId][idx];
  }

  //update locally, ghosts need to be explicitly synchronized
  void updateAt(size_t idx, double  value, size_t sid) {
    //size_t sid = config->threadToSocket(tid);
    size_t offset = starts[sid];
    if (idx >= offset && idx < ends[sid]) wrapper[sid][idx-offset] = value;
    //else throw runtime_exception
  }

  void unsafe_update(size_t socketId, size_t idx, double  value) {
    wrapper[socketId][idx] = value;
  }
#endif
*/
};

#endif
