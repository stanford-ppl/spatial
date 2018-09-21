#ifndef __cppRefint32_t__
#define __cppRefint32_t__

#include "DeliteNamespaces.h"
class cppRefint32_t {
public:
  int32_t  data;

  cppRefint32_t(int32_t  _data) {
    data = _data;
  }

  int32_t  get(void) {
    return data;
  }

  void set(int32_t  newVal) {
      data = newVal;
  }
};

struct cppRefint32_tD {
  void operator()(cppRefint32_t *p) {
    //printf("cppRefint32_t: deleting %p\n",p);
  }
};

#endif
