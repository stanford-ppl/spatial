#ifndef __cppRefcppDeliteArraycppDeliteArrayint32_t__
#define __cppRefcppDeliteArraycppDeliteArrayint32_t__

#include "DeliteNamespaces.h"
class cppRefcppDeliteArraycppDeliteArrayint32_t {
public:
  cppDeliteArraycppDeliteArrayint32_t * data;

  cppRefcppDeliteArraycppDeliteArrayint32_t(cppDeliteArraycppDeliteArrayint32_t * _data) {
    data = _data;
  }

  cppDeliteArraycppDeliteArrayint32_t * get(void) {
    return data;
  }

  void set(cppDeliteArraycppDeliteArrayint32_t * newVal) {
      data = newVal;
  }
};

struct cppRefcppDeliteArraycppDeliteArrayint32_tD {
  void operator()(cppRefcppDeliteArraycppDeliteArrayint32_t *p) {
    //printf("cppRefcppDeliteArraycppDeliteArrayint32_t: deleting %p\n",p);
  }
};

#endif
