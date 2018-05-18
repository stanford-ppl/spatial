#ifndef __DELITE_CPP_PROFILER_H__
#define __DELITE_CPP_PROFILER_H__

#include <map>
#include <vector>
#include <string>
#include <iostream>
#include <ios>
#include <fstream>
#include <sys/time.h>
#include <stdint.h>
#include <stack>
#include <stdio.h>
#include <ctime>
#include <cstring>
#include <stdexcept>
#include <unistd.h>
#include <sstream>
#include <cstdlib>
#include <iomanip>
#include "DeliteNamespaces.h"
#include "pcmHelper.h"
#include <jni.h>

typedef struct {
  std::string name;
  double start;
} cpptimer_t;

typedef struct {
  void* startAddr;
  uint64_t size;
} cpparray_layout_info;

#ifndef DELITE_NUM_CUDA

class BufferedFileWriter {

  public:
    BufferedFileWriter(const char* fileName);
    void writeTimer(std::string kernel, long start, double duration, int32_t level, int32_t tid, bool isMultiLoop);
    void close();

  private:
    std::ofstream fs; 
};

#endif

void InitDeliteCppTimer(int32_t lowestCppTid, int32_t numCppThreads);
void DeliteCppTimerTic(string name);
void DeliteCppTimerToc(string name);
void DeliteCppTimerStart(int32_t tid, string name);
void DeliteCppTimerStop(int32_t tid, string name);
void DeliteCppTimerStopMultiLoop(int32_t tid, string name);
void DeliteCppTimerClose();
void DeliteUpdateMemoryAccessStats( int32_t tid, std::string sourceContext, PCMStats* stats );
void DeliteSendMemoryAccessStatsToJVM( int32_t offset, JNIEnv* env );
void DeliteSendStartTimeToJVM( JNIEnv* env );
void SendKernelMemUsageStatsToJVM( JNIEnv* env );
void DeliteLogArrayAllocation(int32_t tid, void* startAddr, int32_t length, std::string elemType, std::string sourceContext);

#endif
