#ifndef __cppRefcppDeliteArrayint32_t__
#define __cppRefcppDeliteArrayint32_t__

#include "DeliteNamespaces.h"
class cppRefcppDeliteArrayint32_t {
public:
  cppDeliteArrayint32_t * data;

  cppRefcppDeliteArrayint32_t(cppDeliteArrayint32_t * _data) {
    data = _data;
  }

  cppDeliteArrayint32_t * get(void) {
    return data;
  }

  void set(cppDeliteArrayint32_t * newVal) {
      data = newVal;
  }
};

struct cppRefcppDeliteArrayint32_tD {
  void operator()(cppRefcppDeliteArrayint32_t *p) {
    //printf("cppRefcppDeliteArrayint32_t: deleting %p\n",p);
  }
};

#endif
