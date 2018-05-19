#ifndef __cppRefcppDeliteArraystring__
#define __cppRefcppDeliteArraystring__

#include "DeliteNamespaces.h"
class cppRefcppDeliteArraystring {
public:
  cppDeliteArraystring * data;

  cppRefcppDeliteArraystring(cppDeliteArraystring * _data) {
    data = _data;
  }

  cppDeliteArraystring * get(void) {
    return data;
  }

  void set(cppDeliteArraystring * newVal) {
      data = newVal;
  }
};

struct cppRefcppDeliteArraystringD {
  void operator()(cppRefcppDeliteArraystring *p) {
    //printf("cppRefcppDeliteArraystring: deleting %p\n",p);
  }
};

#endif
