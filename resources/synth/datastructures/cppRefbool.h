#ifndef __cppRefbool__
#define __cppRefbool__

#include "DeliteNamespaces.h"
class cppRefbool {
public:
  bool  data;

  cppRefbool(bool  _data) {
    data = _data;
  }

  bool  get(void) {
    return data;
  }

  void set(bool  newVal) {
      data = newVal;
  }
};

struct cppRefboolD {
  void operator()(cppRefbool *p) {
    //printf("cppRefbool: deleting %p\n",p);
  }
};

#endif
