#ifndef __cppRefcppDeliteArraybool__
#define __cppRefcppDeliteArraybool__

#include "DeliteNamespaces.h"
class cppRefcppDeliteArraybool {
public:
  cppDeliteArraybool * data;

  cppRefcppDeliteArraybool(cppDeliteArraybool * _data) {
    data = _data;
  }

  cppDeliteArraybool * get(void) {
    return data;
  }

  void set(cppDeliteArraybool * newVal) {
      data = newVal;
  }
};

struct cppRefcppDeliteArrayboolD {
  void operator()(cppRefcppDeliteArraybool *p) {
    //printf("cppRefcppDeliteArraybool: deleting %p\n",p);
  }
};

#endif
