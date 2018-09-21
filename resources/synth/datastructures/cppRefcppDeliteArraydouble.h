#ifndef __cppRefcppDeliteArraydouble__
#define __cppRefcppDeliteArraydouble__

#include "DeliteNamespaces.h"
class cppRefcppDeliteArraydouble {
public:
  cppDeliteArraydouble * data;

  cppRefcppDeliteArraydouble(cppDeliteArraydouble * _data) {
    data = _data;
  }

  cppDeliteArraydouble * get(void) {
    return data;
  }

  void set(cppDeliteArraydouble * newVal) {
      data = newVal;
  }
};

struct cppRefcppDeliteArraydoubleD {
  void operator()(cppRefcppDeliteArraydouble *p) {
    //printf("cppRefcppDeliteArraydouble: deleting %p\n",p);
  }
};

#endif
