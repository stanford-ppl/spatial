#ifndef __MultiLoopSync_h__
#define __MultiLoopSync_h__

#include <pthread.h>
#include "DeliteDatastructures.h"

extern resourceInfo_t* resourceInfos;

template <class T>
class MultiLoopSync {
private:
  int64_t _loopSize;
  int32_t _numChunksHint;
  resourceInfo_t *_resourceInfo;
  int32_t _numThreads;
  int32_t _numChunks;

  //dynamic scheduler;
  resourceInfo_t *threads;

  pthread_mutex_t offset_lock;
  int32_t offset;

  //synchronous buffers for passing activation records between threads
  T *results; T *resultsC; T *resultsP;
  pthread_mutex_t *lock; pthread_mutex_t *lockC; pthread_mutex_t *lockP;
  pthread_cond_t *cond; pthread_cond_t *condC; pthread_cond_t *condP;
  bool *notReady; bool *notReadyC; bool *notReadyP;

  void initThreads() {
  	int32_t availableThreads = _resourceInfo->availableThreads;
  	int32_t start = _resourceInfo->threadId;
  	int32_t end = start + availableThreads; //default: use all threads
  	int32_t step = 1;

  	if (_loopSize == 0) end = start+1; //1 thread
  	else if (_loopSize < availableThreads) step = availableThreads / (int32_t)_loopSize; //spread threads out over available range

  	_numThreads = (end-start)/step;
  	if ((end-start) % step != 0) _numThreads++;
  	int32_t newAvailable = availableThreads / _numThreads;
  	threads = new resourceInfo_t[_numThreads];
  	
    //fprintf(stderr, "loopSize: %d, start: %d, available: %d\n", _loopSize, start, newAvailable);

  	int idx = 0;
  	for (int id = start; id < end; id += step) {
  	  threads[idx] = resourceInfos[id]; //copy constructor
      threads[idx].groupId = idx;
      threads[idx].groupSize = _numThreads;
  	  threads[idx].availableThreads = newAvailable;
  	  idx++;
  	}
  }

  void initChunks() {
    int32_t defaultChunks;
    if (_numChunksHint == -1) defaultChunks = (int32_t)(_loopSize/(log10((double)_loopSize)*(500.0/_numThreads))); 
    else defaultChunks = _numChunksHint;

    if (_numThreads == 1 || defaultChunks < _numThreads || _loopSize < defaultChunks) _numChunks = _numThreads;
    else _numChunks = defaultChunks;
  }

  void initBuffers() {
    offset = _numThreads;
    pthread_mutex_init(&offset_lock, NULL);

    results = new T[_numChunks]; resultsC = new T[_numThreads]; resultsP = new T[_numThreads];
    lock = new pthread_mutex_t[_numChunks]; lockC = new pthread_mutex_t[_numThreads]; lockP = new pthread_mutex_t[_numThreads]; 
    cond = new pthread_cond_t[_numChunks]; condC = new pthread_cond_t[_numThreads]; condP = new pthread_cond_t[_numThreads]; 
    notReady = new bool[_numChunks]; notReadyC = new bool[_numThreads]; notReadyP = new bool[_numThreads];

    for(int i=0; i<_numChunks; i++) {
      pthread_mutex_init(&lock[i], NULL);
      pthread_cond_init(&cond[i], NULL);
      notReady[i] = true;
    }

    for(int i=0; i<_numThreads; i++) {
      pthread_mutex_init(&lockC[i], NULL);
      pthread_cond_init(&condC[i], NULL);
      notReadyC[i] = true;

      pthread_mutex_init(&lockP[i], NULL);
      pthread_cond_init(&condP[i], NULL);
      notReadyP[i] = true;
    }
  }

public:

  MultiLoopSync(int64_t loopSize, int32_t numChunksHint, resourceInfo_t *resourceInfo) {
    _loopSize = loopSize;
    _numChunksHint = numChunksHint;
    _resourceInfo = resourceInfo;

    initThreads();
    initChunks();
    initBuffers();
  }

  int32_t numChunks() { return _numChunks; }

  int32_t numThreads() { return _numThreads; }
 
  resourceInfo_t *getThreadResource(int32_t idx) { return &threads[idx]; }

  //dynamic scheduler
  int32_t getNextChunkIdx() {
    int32_t idx;
    pthread_mutex_lock(&offset_lock);
    idx = offset;
    offset += 1;
    pthread_mutex_unlock(&offset_lock);
    return idx;
  }


  //barrier between stages
  void awaitBarrier() {
  	delite_barrier(_numThreads);
  }

  
  T get(int32_t i) {
  	pthread_mutex_lock(&lock[i]);
  	while (notReady[i]) {
  	  pthread_cond_wait(&cond[i], &lock[i]);
  	}
  	pthread_mutex_unlock(&lock[i]);
  	return results[i];
  }

  void set(int32_t i, T res) {
  	pthread_mutex_lock(&lock[i]);
  	results[i] = res;
  	notReady[i] = false;
  	pthread_cond_broadcast(&cond[i]);
  	pthread_mutex_unlock(&lock[i]);
  }

  T getC(int32_t i) {
  	pthread_mutex_lock(&lockC[i]);
  	while (notReadyC[i]) {
  	  pthread_cond_wait(&condC[i], &lockC[i]);
  	}
  	pthread_mutex_unlock(&lockC[i]);
  	return resultsC[i];
  }

  void setC(int32_t i, T res) {
  	pthread_mutex_lock(&lockC[i]);
  	resultsC[i] = res;
  	notReadyC[i] = false;
  	pthread_cond_broadcast(&condC[i]);
  	pthread_mutex_unlock(&lockC[i]);
  }

  T getP(int32_t i) {
  	pthread_mutex_lock(&lockP[i]);
  	while (notReadyP[i]) {
  	  pthread_cond_wait(&condP[i], &lockP[i]);
  	}
  	pthread_mutex_unlock(&lockP[i]);
  	return resultsP[i];
  }

  void setP(int32_t i, T res) {
  	pthread_mutex_lock(&lockP[i]);
  	resultsP[i] = res;
  	notReadyP[i] = false;
  	pthread_cond_broadcast(&condP[i]);
  	pthread_mutex_unlock(&lockP[i]);
  }

};
#endif
