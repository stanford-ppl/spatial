/*
#include "cppList.h"

template<> void cppList<int>::release(void) { free(data); }
template<> void cppList<float>::release(void) { free(data); }
template<> void cppList<double>::release(void) { free(data); }
template<> void cppList<bool>::release(void) { free(data); }
template<> void cppList<char>::release(void) { free(data); }
template<> void cppList<short>::release(void) { free(data); }

template<class T> void cppList<T>::release(void) { 
  for(int i=0; i<length; i++)
    data[i]->release(); 
  free(data);
}

*/