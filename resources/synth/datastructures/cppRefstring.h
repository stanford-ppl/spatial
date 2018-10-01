#ifndef __cppRefstring__
#define __cppRefstring__

#include "DeliteNamespaces.h"
class cppRefstring {
public:
  string  data;

  cppRefstring(string  _data) {
    data = _data;
  }

  string  get(void) {
    return data;
  }

  void set(string  newVal) {
      data = newVal;
  }
};

struct cppRefstringD {
  void operator()(cppRefstring *p) {
    //printf("cppRefstring: deleting %p\n",p);
  }
};

#endif
