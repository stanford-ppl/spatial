/*
#include "cppRef.h"

template<> void cppRef<int>::release(void) {  }
template<> void cppRef<float>::release(void) {  }
template<> void cppRef<double>::release(void) { }
template<> void cppRef<bool>::release(void) {  }
template<> void cppRef<char>::release(void) {  }
template<> void cppRef<short>::release(void) {  }

template<class T> void cppRef<T>::release(void) { 
  data->release();
}
*/