/*
#ifndef _CPP_REF_H_
#define _CPP_REF_H_

template <class T>
class cppRef {
public:
    T data;

    cppRef(T _data) {
      data = _data;
    }

    T get(void) {
      return data;
    }

    void set(T newVal) {
        data = newVal;
    }

    void release(void);
    
};

#endif
*/