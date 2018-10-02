/*
#include "cppDeliteArray.h"

template<> void cppDeliteArray<int>::release(void) { free(data); }
template<> void cppDeliteArray<float>::release(void) { free(data); }
template<> void cppDeliteArray<double>::release(void) { free(data); }
template<> void cppDeliteArray<bool>::release(void) { free(data); }
template<> void cppDeliteArray<char>::release(void) { free(data); }
template<> void cppDeliteArray<short>::release(void) { free(data); }
template<> void cppDeliteArray<char*>::release(void) { 
  for(int i=0; i<length; i++)
    free(data[i]);
  free(data);
}

template<class T> void cppDeliteArray<T>::release(void) { 
  for(int i=0; i<length; i++)
    data[i]->release(); 
  free(data);
}

// compiler generated template releases
#include "cppDeliteArrayRelease.h"
*/