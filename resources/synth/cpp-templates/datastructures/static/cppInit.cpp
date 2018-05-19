#include "cppInit.h"

void initializeThread(int threadId) {
  #ifdef __linux__
    cpu_set_t cpu;
    CPU_ZERO(&cpu);
    CPU_SET(threadId, &cpu);
    sched_setaffinity(0, sizeof(cpu_set_t), &cpu);

    #ifdef __DELITE_CPP_NUMA__
      if (numa_available() >= 0) {
        int socketId = config->threadToSocket(threadId);
        if (socketId < numa_num_configured_nodes()) {
          bitmask* nodemask = numa_allocate_nodemask();
          numa_bitmask_setbit(nodemask, socketId);
          numa_set_membind(nodemask);
        }
        //VERBOSE("Binding thread %d to cpu %d, socket %d\n", threadId, threadId, socketId);
      }
    #endif
  #endif

  #ifdef __sun
    processor_bind(P_LWPID, P_MYID, threadId, NULL);
  #endif
}

#ifndef __DELITE_CPP_STANDALONE__
extern "C" JNIEXPORT void JNICALL Java_ppl_delite_runtime_executor_PinnedExecutionThread_initializeThread(JNIEnv* env, jobject obj, jint threadId, jint numThreads);
JNIEXPORT void JNICALL Java_ppl_delite_runtime_executor_PinnedExecutionThread_initializeThread(JNIEnv* env, jobject obj, jint threadId, jint numThreads) {
  initializeThread(threadId);  
}
#endif
