#ifndef __CPPHASHMAP_H__
#define __CPPHASHMAP_H__

#include <stdint.h>
#include <iostream>
#include <algorithm>
#include "DeliteCpp.h"
#define LOADFACTOR_D2 0.2f

//Note: Use unsigned integer types to enable right shift with fill zeros (e.g., hc)

template <class K>
class cppHashMap {
private:
  int32_t *indices;
  uint32_t indices_length;
  K *keys;
  uint32_t keys_length;
  int32_t *blocksizes; // is this used?
  int32_t sz;
  uint32_t relbits;

  static int32_t nextPow2(int32_t x) {
    int32_t c = x - 1;
    c |= c >>  1;
    c |= c >>  2;
    c |= c >>  4;
    c |= c >>  8;
    c |= c >> 16;
    return c + 1;
  }

  // http://stackoverflow.com/questions/7812044/finding-trailing-0s-in-a-binary-number
  static uint32_t numTrailingZeros(uint32_t v) {
    unsigned int c = 32; // c will be the number of zero bits on the right
    v &= -signed(v);
    if (v) c--;
    if (v & 0x0000FFFF) c -= 16;
    if (v & 0x00FF00FF) c -= 8;
    if (v & 0x0F0F0F0F) c -= 4;
    if (v & 0x33333333) c -= 2;
    if (v & 0x55555555) c -= 1;
    return c;
  }

public:

  cppHashMap(int32_t indsz=128, int32_t datasz=52) {
    indices_length = nextPow2(indsz);
    indices = new int[indices_length];
    std::fill_n(indices, indices_length, -1);
    keys_length = datasz;
    keys = new K[datasz]();
    sz = 0;
    relbits = numTrailingZeros(indices_length / 2);
  }
  
  //TODO: differentiate when the memory management is smart pointer
  static cppHashMap<int32_t> *range(int32_t n) {
    cppHashMap<int32_t> *hm = new cppHashMap<int32_t>(n * 5 + 1, n * 3);
    for(int i=0; i<n; i++) hm->put(i);
    return hm;
  }

  int32_t indsz(void) { return indices_length; }

  int32_t datasz(void) { return keys_length; }

  int32_t size(resourceInfo_t *resourceInfo) { return sz; }
  int32_t size(void) { return sz; }

  int32_t get(K key) {
    uint32_t hc = delite_hashcode<K>(key) * 0x9e3775cd; 
    uint32_t relbits0 = relbits;
    uint32_t pos = (hc >> (32 - relbits0)) * 2;
    int32_t currelem = indices[pos];
    int32_t currhash = indices[pos + 1];

    int32_t mask = indices_length - 1;
    while (currelem != -1 && ((currhash != hc) || !(delite_equals(keys[currelem],key)))) {
      pos = (pos + 2) & mask;
      currelem = indices[pos];
      currhash = indices[pos + 1];
    }
    
    return currelem;
  }

  int32_t put(K key) {
    uint32_t hc = delite_hashcode<K>(key) * 0x9e3775cd;
    uint32_t relbits0 = relbits;
    uint32_t pos = (hc >> (32 - relbits0)) * 2;
    int32_t currelem = indices[pos];
    int32_t currhash = indices[pos + 1];
    
    int32_t mask = indices_length - 1;
    while (currelem != -1 && ((currhash != hc) || !(delite_equals(keys[currelem],key)))) {
      pos = (pos + 2) & mask;
      currelem = indices[pos];
      currhash = indices[pos + 1];
    }
    
    if (currelem == -1) {
      int32_t datapos = sz;
      indices[pos] = datapos;
      indices[pos + 1] = hc;
      keys[datapos] = key;
      sz += 1;
      
      grow();
      return datapos;
    } else {
      int32_t datapos = currelem;
      keys[datapos] = key;
      return datapos;
    }
  }
  
  void statistics(void) {
    std::cout << "size: " << sz << std::endl;
    std::cout << "indices length: " << indices_length << std::endl;
    std::cout << "data length: " << keys_length << std::endl;
    std::cout << "growth threashold: " << (int32_t)(LOADFACTOR_D2*indices_length) << std::endl;
  }

  void grow(void) {
    if (sz <= (LOADFACTOR_D2 * indices_length)) return;
    int32_t *nindices = new int32_t[indices_length * 2];
    std::fill_n(nindices, indices_length*2, -1);;
    K *nkeys = new K[keys_length * 2]();
    relbits = numTrailingZeros(indices_length);
    int32_t mask = 2 * indices_length - 1;

    // copy raw data
    std::copy(keys, keys+sz, nkeys);
    
    // copy indices
    int32_t i = 0;
    int32_t relbits0 = relbits;
    while (i < indices_length) {
      int32_t elem = indices[i];
      if (elem != -1) {
        uint32_t hash = indices[i + 1];
        uint32_t pos = (hash >> (32 - relbits0)) * 2;
        
        // insert it into nindices
        int32_t currelem = nindices[pos];
        int32_t currhash = nindices[pos + 1];
        while (currelem != -1) {
          pos = (pos + 2) & mask;
          currelem = nindices[pos];
          currhash = nindices[pos + 1];
        }
        nindices[pos] = elem;
        nindices[pos + 1] = hash;
      }
      i += 2;
    }
    
    //TODO: check with rest of the memory management
    delete[] indices;
    delete[] keys;

    indices = nindices;
    indices_length = indices_length * 2;
    keys = nkeys;
    keys_length = keys_length * 2;

  }
  
  K *unsafeKeys(resourceInfo_t *resourceInfo) { return keys; }
  K *unsafeKeys(void) { return keys; }

  uint32_t hashcode(void) { return (uintptr_t)this; }

  // currently not used
  /*
  int32_t *unsafeIndices(void) { return indices };
  
  int32_t unsafeSize(void) = { return sz; }
  
  int32_t unsafeBlockSizes = { return blocksizes; }
  
  void unsafeSetBlockSizes(int32_t *_blkszs) = { blocksizes = _blkszs; }
  
  void unsafeSetKeys(K *_keys) { keys = _keys; }
  
  void unsafeSetSize(int32_t _sz) { sz = _sz; }
  
  void unsafeSetInternal(int32_t *_ind, K *_keys, int32_t _sz) {
    indices = _ind
    keys = _keys
    sz = _sz
  }
  */
};

/* template partial specialization for pointer types */
template <class K>
class cppHashMap<K*> {
private:
  int32_t *indices;
  uint32_t indices_length;
  K **keys;
  uint32_t keys_length;
  int32_t *blocksizes; // is this used?
  int32_t sz;
  uint32_t relbits;

  static int32_t nextPow2(int32_t x) {
    int32_t c = x - 1;
    c |= c >>  1;
    c |= c >>  2;
    c |= c >>  4;
    c |= c >>  8;
    c |= c >> 16;
    return c + 1;
  }

  // http://stackoverflow.com/questions/7812044/finding-trailing-0s-in-a-binary-number
  static uint32_t numTrailingZeros(uint32_t v) {
    unsigned int c = 32; // c will be the number of zero bits on the right
    v &= -signed(v);
    if (v) c--;
    if (v & 0x0000FFFF) c -= 16;
    if (v & 0x00FF00FF) c -= 8;
    if (v & 0x0F0F0F0F) c -= 4;
    if (v & 0x33333333) c -= 2;
    if (v & 0x55555555) c -= 1;
    return c;
  }

public:

  cppHashMap(int32_t indsz=128, int32_t datasz=52) {
    indices_length = nextPow2(indsz);
    indices = new int[indices_length];
    std::fill_n(indices, indices_length, -1);
    keys_length = datasz;
    keys = new K*[datasz]();
    sz = 0;
    relbits = numTrailingZeros(indices_length / 2);
  }
  
  //TODO: differentiate when the memory management is smart pointer
  static cppHashMap<int32_t> *range(int32_t n) {
    cppHashMap<int32_t> *hm = new cppHashMap<int32_t>(n * 5 + 1, n * 3);
    for(int i=0; i<n; i++) hm->put(i);
    return hm;
  }

  int32_t indsz(void) { return indices_length; }

  int32_t datasz(void) { return keys_length; }

  int32_t size(resourceInfo_t *resourceInfo) { return sz; }
  int32_t size(void) { return sz; }

  int32_t get(K *key) {
    uint32_t hc = key->hashcode() * 0x9e3775cd; 
    uint32_t relbits0 = relbits;
    uint32_t pos = (hc >> (32 - relbits0)) * 2;
    int32_t currelem = indices[pos];
    int32_t currhash = indices[pos + 1];

    int32_t mask = indices_length - 1;
    while (currelem != -1 && ((currhash != hc) || !(keys[currelem]->equals(key)))) {
      pos = (pos + 2) & mask;
      currelem = indices[pos];
      currhash = indices[pos + 1];
    }
    
    return currelem;
  }

  int32_t put(K *key) {
    uint32_t hc = key->hashcode() * 0x9e3775cd;
    uint32_t relbits0 = relbits;
    uint32_t pos = (hc >> (32 - relbits0)) * 2;
    int32_t currelem = indices[pos];
    int32_t currhash = indices[pos + 1];
    
    int32_t mask = indices_length - 1;
    while (currelem != -1 && ((currhash != hc) || !(keys[currelem]->equals(key)))) {
      pos = (pos + 2) & mask;
      currelem = indices[pos];
      currhash = indices[pos + 1];
    }
    
    if (currelem == -1) {
      int32_t datapos = sz;
      indices[pos] = datapos;
      indices[pos + 1] = hc;
      keys[datapos] = key;
      sz += 1;
      
      grow();
      return datapos;
    } else {
      int32_t datapos = currelem;
      keys[datapos] = key;
      return datapos;
    }
  }
  
  void statistics(void) {
    std::cout << "size: " << sz << std::endl;
    std::cout << "indices length: " << indices_length << std::endl;
    std::cout << "data length: " << keys_length << std::endl;
    std::cout << "growth threashold: " << (int32_t)(LOADFACTOR_D2*indices_length) << std::endl;
  }

  void grow(void) {
    if (sz <= (LOADFACTOR_D2 * indices_length)) return;
    int32_t *nindices = new int32_t[indices_length * 2];
    std::fill_n(nindices, indices_length*2, -1);;
    K **nkeys = new K*[keys_length * 2]();
    relbits = numTrailingZeros(indices_length);
    int32_t mask = 2 * indices_length - 1;

    // copy raw data
    std::copy(keys, keys+sz, nkeys);
    
    // copy indices
    int32_t i = 0;
    int32_t relbits0 = relbits;
    while (i < indices_length) {
      int32_t elem = indices[i];
      if (elem != -1) {
        uint32_t hash = indices[i + 1];
        uint32_t pos = (hash >> (32 - relbits0)) * 2;
        
        // insert it into nindices
        int32_t currelem = nindices[pos];
        int32_t currhash = nindices[pos + 1];
        while (currelem != -1) {
          pos = (pos + 2) & mask;
          currelem = nindices[pos];
          currhash = nindices[pos + 1];
        }
        nindices[pos] = elem;
        nindices[pos + 1] = hash;
      }
      i += 2;
    }
    
    //TODO: check with rest of the memory management
    delete[] indices;
    delete[] keys;

    indices = nindices;
    indices_length = indices_length * 2;
    keys = nkeys;
    keys_length = keys_length * 2;

  }
  
  K **unsafeKeys(resourceInfo_t *resourceInfo) { return keys; }
  K **unsafeKeys(void) { return keys; }

  uint32_t hashcode(void) { return (uintptr_t)this; }
};

template <class T>
class cppBucket {
public:
  T *array;
  int32_t size;
  
  cppBucket() {
    array = NULL;
    size = 0;
  }
  
  T dcApply(int32_t idx) { return array[idx]; }
  void dcUpdate(int32_t idx, T x) { array[idx] = x; }

  void toString(void) {
    std::cout << "cppBucket(size: " << size << ", values: ";
    for(int i=0; i<size; i++) 
      std::cout << array[i];
    std::cout << std::endl;
  }
};

#endif
