#ifndef __cppDeliteArrayint32_t__
#define __cppDeliteArrayint32_t__

#include "DeliteNamespaces.h"
#include "DeliteMemory.h"
#ifdef __DELITE_CPP_NUMA__
#include <numa.h>
#endif

class cppDeliteArrayint32_t : public DeliteMemory {
public:
  int32_t  *data;
  int length;

  cppDeliteArrayint32_t(int _length, resourceInfo_t *resourceInfo): data((int32_t  *)(new (resourceInfo) int32_t [_length])), length(_length) { }

  cppDeliteArrayint32_t(int _length): data((int32_t  *)(new int32_t [_length])), length(_length) { }

  cppDeliteArrayint32_t(int32_t  *_data, int _length) {
    data = _data;
    length = _length;
  }

  int32_t  apply(int idx) {
    return data[idx];
  }

  void update(int idx, int32_t  val) {
    data[idx] = val;
  }

  void print(void) {
    printf("length is %d\n", length);
  }

  bool equals(cppDeliteArrayint32_t *to) {
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

struct cppDeliteArrayint32_tD {
  void operator()(cppDeliteArrayint32_t *p) {
    //printf("cppDeliteArrayint32_t: deleting %p\n",p);
    delete[] p->data;
  }

};

#endif
