/*
#ifndef _CPP_LIST_H_
#define _CPP_LIST_H_

#include <stdlib.h>
#include <string.h>

template <class T>
class cppList {
public:
    T *data;
    int length;

    // Constructor
    cppList(void) {
      length = 0;
      data = NULL;
    }

    cppList(int _length) {
        length = _length;
        data = (T *)malloc(length*sizeof(T));
    }

    cppList(T *_data, int _length) {
        length = _length;
        data = _data;
    }

    T apply(int idx) {
        return data[idx];
    }

    void update(int idx, T value) {
        data[idx] = value;
    }

    // DeliteCoolection
    int size() {
        return length;
    }

    T dcApply(int idx) {
        return data[idx];
    }

    void dcUpdate(int idx, T value) {
        data[idx] = value;
    }

    // Additional functions
    void copy(int srcOffset, cppList<T> *dest, int destOffset, int length) {
      memcpy(dest->data + destOffset, data + srcOffset, sizeof(T) * length);
    }

    cppList<T> *arrayunion(cppList<T> *rhs) {
      int newLength = length + rhs->length;
      cppList<T> *result = new cppList<T>(newLength);
      int acc = 0;
      for(int i=0; i<length; i++) {
        T elem = data[i];
        int j = 0;
        while(j < acc) {
          if(elem == result->data[j]) break;
          j += 1;
        }
        if(j == acc) result->data[acc++] = elem;
      }
      for(int i=0; i<rhs->length; i++) {
        T elem = rhs->data[i];
        int j = 0;
        while(j < acc) {
          if(elem == result->data[j]) break;
          j += 1;
        }
        if(j == acc) result->data[acc++] = elem;
      }
      result->length = acc-1;
      //TODO: Need to shrink the actual array size?
      return result;
    }

    cppList<T> *intersect(cppList<T> *rhs) {
      int newLength = max(length, rhs->length);
      cppList<T> *result = new cppList<T>(newLength);
      int acc = 0;
      for(int i=0; i<length; i++)
        for(int j=0; j<rhs->length; j++) 
          if(data[i] == rhs->data[j]) result->data[acc++] = data[i];
      result->length = acc-1;
      //TODO: Need to shrink the actual array size?
      return result;
    }
    
    cppList<T> *take(int n) {
      cppList<T> *result = new cppList<T>(n);
      memcpy(result->data, data, sizeof(T) * n);
      return result;
    }

    void release(void);

};

#endif
*/