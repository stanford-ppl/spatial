#include <pthread.h>
#include "DeliteCpp.h"


pthread_t* threadPool;
pthread_mutex_t* locks;
pthread_cond_t* readyConds;
pthread_cond_t* doneConds;

void** workPool;
void** argPool;

void submitWork(int threadId, void *(*work) (void *), void *arg) {
  pthread_mutex_lock(&locks[threadId]);
  while (argPool[threadId] != NULL) {
  	pthread_cond_wait(&doneConds[threadId], &locks[threadId]);
  }
  workPool[threadId] = (void*)work;
  argPool[threadId] = arg;
  pthread_cond_signal(&readyConds[threadId]);
  pthread_mutex_unlock(&locks[threadId]);
}

void* processWork(void* threadId) {
  int id = *(int*)threadId;
  VERBOSE("Initialized thread with id %d\n", id);
  initializeThread(id);

  void *(*work) (void *);
  void *arg;
  while(true) {
  	pthread_mutex_lock(&locks[id]);
  	while (argPool[id] == NULL) {
  	  pthread_cond_wait(&readyConds[id], &locks[id]);
  	}
  	work = (void *(*)(void *))workPool[id];
  	workPool[id] = NULL;
  	arg = argPool[id];
    argPool[id] = NULL;
  	pthread_cond_signal(&doneConds[id]);
  	pthread_mutex_unlock(&locks[id]);
  	work(arg);
  }
}

void initializeThreadPool(int numThreads) {
  threadPool = new pthread_t[numThreads];
  locks = new pthread_mutex_t[numThreads];
  readyConds = new pthread_cond_t[numThreads];
  doneConds = new pthread_cond_t[numThreads];
  workPool = new void*[numThreads];
  argPool = new void*[numThreads];

  for (int i=1; i<numThreads; i++) {
  	pthread_mutex_init(&locks[i], NULL);
  	pthread_cond_init(&readyConds[i], NULL);
  	pthread_cond_init(&doneConds[i], NULL);
  	workPool[i] = NULL;
  	argPool[i] = NULL;
    pthread_create(&threadPool[i], NULL, processWork, (void*)&(resourceInfos[i].threadId));
  }
}
