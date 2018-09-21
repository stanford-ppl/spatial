#include "DeliteCppProfiler.h"

static std::vector< std::map<std::string,std::vector<cpptimer_t>*>* > *timermaps;
static std::vector< std::stack< cpptimer_t > > kernelCallStacks;
static std::vector< BufferedFileWriter* >* profileWriters;
static std::vector< std::map<std::string, std::vector<PCMStats*>*>* > *memoryAccessMaps;
static std::vector< std::map< std::string, std::vector<cpparray_layout_info>* >* >* scToMemAllocationMaps;
static std::map< std::string, cpptimer_t >* ticTocRegionToTimers;
static std::vector< std::map< std::string, uint64_t >* >* kernelToMemUsageMaps;

static std::string profileFilePrefix;
static std::string ticTocProfileFile;

static int32_t numCpp = 0;
static int32_t lowestCppTid = 0;
static double appStartTime;

double milliseconds(struct timeval t) {
  return double(t.tv_sec * 1000) + (double(t.tv_usec) / 1000);
}

void setFilePaths() {
  char cwd[1024];
  getcwd(cwd, 1024);

  std::stringstream ss;
  ss << cwd << "/profile/profile_t_";
  profileFilePrefix = ss.str();
  
  ss.str("");
  ss << cwd << "/profile/profile_tic_toc_cpp.csv";
  ticTocProfileFile = ss.str();
}

void InitDeliteCppTimer(int32_t _lowestCppTid, int32_t numCppThreads) {
  struct timeval a;
  gettimeofday(&a,NULL);
  appStartTime = milliseconds(a);

  setFilePaths();

  numCpp = numCppThreads;
  lowestCppTid = _lowestCppTid;
  memoryAccessMaps = new std::vector< std::map< std::string, std::vector<PCMStats*>* >* >;
  ticTocRegionToTimers = new  std::map< std::string, cpptimer_t >;
  scToMemAllocationMaps = new std::vector< std::map< std::string, std::vector<cpparray_layout_info>* >* >;
  kernelToMemUsageMaps = new std::vector< std::map< std::string, uint64_t >* >;
  profileWriters = new std::vector< BufferedFileWriter* >; 

  for (int32_t i = 0; i < numCppThreads; i++) {
    memoryAccessMaps->push_back(new std::map< std::string, std::vector<PCMStats*>* >());
    scToMemAllocationMaps->push_back(new std::map< std::string, std::vector<cpparray_layout_info>* >());
    kernelToMemUsageMaps->push_back(new std::map< std::string, uint64_t >());

    std::stack< cpptimer_t > s;
    kernelCallStacks.push_back(s);

    std::stringstream ss;
    ss << profileFilePrefix << (lowestCppTid + i) << ".csv";
	  profileWriters->push_back( new BufferedFileWriter(ss.str().c_str()) );
  }

  profileWriters->push_back( new BufferedFileWriter(ticTocProfileFile.c_str()) );
}

void DeliteCppTimerTic(string name) {
  struct timeval start;
  gettimeofday(&start,NULL);
  std::string n = std::string(name.c_str());
  cpptimer_t timer = {n, milliseconds(start)};
  
  ticTocRegionToTimers->insert( std::pair< std::string, cpptimer_t >( n, timer ));
}

void DeliteCppTimerToc(string name) {
  struct timeval t;
  gettimeofday(&t,NULL);

  std::string n = std::string(name.c_str());
  std::map< std::string, cpptimer_t >::iterator it = ticTocRegionToTimers->find(n);

  cpptimer_t timer = it->second;
  double end = milliseconds(t);
  double elapsedMillis = end - timer.start;
  profileWriters->at(numCpp)->writeTimer(timer.name, long(timer.start - appStartTime), elapsedMillis, 0, numCpp, false);
  ticTocRegionToTimers->erase(it);

  std::cout << "[METRICS]: Time for component " << timer.name << ": " << std::fixed << std::setprecision(3) << (elapsedMillis/1000.0) << "s" << std::endl;
}

void deliteCppTimerStopHelper(int32_t tid, string _name, bool isMultiLoop = false) {
  struct timeval t;
  gettimeofday(&t,NULL);

  cpptimer_t timer = kernelCallStacks[tid].top();

  std::string name = std::string(_name.c_str());
  if (timer.name != name) {
   	  throw std::runtime_error("The kernelCallStack is in an invalid state. The top of stack does not match the kernel to be stopped.\n");
  }

  double end = milliseconds(t);
  double elapsedMillis = end - timer.start;
  kernelCallStacks[tid].pop();
  profileWriters->at(tid)->writeTimer(timer.name, long(timer.start - appStartTime), elapsedMillis, kernelCallStacks[tid].size(), tid, isMultiLoop);
}

void DeliteCppTimerStopMultiLoop(int32_t tid, string name) {
  deliteCppTimerStopHelper(tid, name, true);
}

void DeliteCppTimerStart(int32_t tid, string name) {
  struct timeval start;
  gettimeofday(&start,NULL);
  cpptimer_t timer = { std::string(name.c_str()), milliseconds(start) };
  kernelCallStacks[tid].push(timer);
}

void DeliteCppTimerStop(int32_t tid, string name) {
  deliteCppTimerStopHelper(tid, name);
}

void DeliteCppTimerClose() {
  for (int32_t i = 0; i <= numCpp; i++) {
    profileWriters->at(i)->close();
  }

  free(memoryAccessMaps);
  free(ticTocRegionToTimers);
  free(scToMemAllocationMaps);
  free(kernelToMemUsageMaps);
  free(profileWriters);
}

uint64_t estimateSizeOfArray(unsigned long length, std::string elemType) {
  unsigned int elemSize = 0;
  if ( (elemType == "boolean") || (elemType == "byte") ) {
  	elemSize = 1;
  } else if ( (elemType == "char") || (elemType == "short") ) {
	elemSize = 2;
  } else if ( (elemType == "int") || (elemType == "float") ) {
	elemSize = 4;
  } else {
	elemSize = 8;
  }

  return length * elemSize;
}

void incMemUsageOfKernel( int32_t tid, std::string kernel, uint64_t inc ) {
  std::map< std::string, uint64_t >* m = kernelToMemUsageMaps->at(tid);
  std::map< std::string, uint64_t >::iterator it = m->find( kernel );
  if (it == m->end()) {
    std::pair< std::string, uint64_t > kv( kernel, inc );
    m->insert(kv);
  } else {
    it->second += inc; 
  }
}

void DeliteLogArrayAllocation(int32_t tid, void* startAddr, int32_t length, std::string elemType, std::string sourceContext) {
  if (!kernelCallStacks[tid].empty()) {
    std::string currKernel = kernelCallStacks[tid].top().name;
    std::map< std::string, std::vector<cpparray_layout_info>* >* m = scToMemAllocationMaps->at(tid);
    std::map< std::string, std::vector<cpparray_layout_info>* >::iterator it = m->find( sourceContext );
    if ( it == m->end() ) {
  	  std::pair< std::string, std::vector<cpparray_layout_info>* > p (sourceContext, new std::vector<cpparray_layout_info>);
  	  m->insert(p);
    }

    uint64_t size = estimateSizeOfArray(length, elemType);
    incMemUsageOfKernel( tid, currKernel, size );
    
    cpparray_layout_info l = { startAddr, size };
    m->find(sourceContext)->second->push_back(l);
  }
}

void dumpSourceContextToId( std::map< std::string, int32_t >* scToId ) {
  std::ofstream outFile;
  if (!scToId->empty()) outFile.open("scToId.csv", std::ofstream::out);

  std::map< std::string, int32_t >::iterator it;
  for (it = scToId->begin(); it != scToId->end(); it++) {
    outFile << it->second << "," << it->first << std::endl; 
  }

  if (outFile.is_open()) outFile.close();
}

void dumpArrayAddrRanges() {
  std::map< std::string, int32_t >* scToId = new std::map< std::string, int32_t >();
  std::ofstream outFile;

  std::vector< std::map< std::string, std::vector<cpparray_layout_info>* >* >::iterator it;
  int numThreads = scToMemAllocationMaps->size();
  for (int32_t tid = 0; tid < numThreads; tid++) {
    std::map< std::string, std::vector<cpparray_layout_info>* >* m = scToMemAllocationMaps->at(tid);
  	std::map< std::string, std::vector<cpparray_layout_info>* >::iterator it1;
	for (it1 = m->begin(); it1 != m->end(); it1++) {
	  std::string sc = std::string(it1->first.c_str());
      std::map< std::string, int32_t >::iterator tmp = scToId->find(sc);
	  int scId = scToId->size();
	  if (tmp == scToId->end()) {
	    scToId->insert(std::pair< std::string, int32_t >(sc, scId));
	  } else {
	    scId = tmp->second;
	  } 

	  std::vector<cpparray_layout_info>* v = it1->second; 
      std::vector<cpparray_layout_info>::iterator it2;
      for (it2 = v->begin(); it2 != v->end(); it2++) {
	    uint64_t s = (uint64_t) it2->startAddr;
        if (!outFile.is_open()) outFile.open("dataStructures.csv", std::ofstream::out);
        outFile << "0x" << std::hex << s << "," << std::dec << it2->size << "," << scId << std::endl;
      }
	}
  }

  if (outFile.is_open()) outFile.close();
  dumpSourceContextToId( scToId );
}

void DeliteUpdateMemoryAccessStats( int32_t tid, std::string sourceContext, PCMStats* stats ) {
    std::map< std::string, std::vector<PCMStats*>* >* scToMemoryAccessStats = memoryAccessMaps->at(tid);
    std::map< std::string, std::vector<PCMStats*>* >::iterator it = scToMemoryAccessStats->find( sourceContext );
    if (it == scToMemoryAccessStats->end()) {
        std::vector<PCMStats*>* v = new std::vector<PCMStats*>();
        v->push_back(stats);
        scToMemoryAccessStats->insert( std::pair< std::string, std::vector<PCMStats*>* >( sourceContext, v));
    } else {
        it->second->push_back( stats );
    }
}

void DeliteSendStartTimeToJVM( JNIEnv* env ) {
    #ifndef __DELITE_CPP_STANDALONE__
    jclass cls = env->FindClass("ppl/delite/runtime/profiler/PerformanceTimer");
    jmethodID mid = env->GetStaticMethodID(cls, "setCppStartTime", "(J)V");

    if (mid == NULL) {
        std::cout << "Could not find method" << std::endl;
        return;
    }

	jdouble start = appStartTime;
	env->CallStaticVoidMethod(cls, mid, start);
	#endif
}

void DeliteSendMemoryAccessStatsToJVM( int32_t offset, JNIEnv* env ) {
    #ifndef __DELITE_CPP_STANDALONE__
    jclass cls = env->FindClass("ppl/delite/runtime/profiler/MemoryProfiler");
    jmethodID mid = env->GetStaticMethodID(cls, "addMemoryAccessStats", "(Ljava/lang/String;IDIDI)V");

    if (mid == NULL) {
        std::cout << "Could not find method" << std::endl;
        return;
    }

    for (int32_t tid=0; tid < memoryAccessMaps->size(); tid++) {
        std::map< std::string, std::vector<PCMStats*>*> * scToMemoryAccessStats = memoryAccessMaps->at(tid);
        std::map< std::string, std::vector<PCMStats*>*>::iterator it;

        for (it = scToMemoryAccessStats->begin(); it != scToMemoryAccessStats->end(); it++) {
            std::string sc = it->first;
            std::vector<PCMStats*>* statsVector = it->second;
            PCMStats* stats = statsVector->at(0); // HACK: First question is: Do we need a vector

            jstring sourceContext = env->NewStringUTF(sc.c_str());
            jint l2Misses = stats->l2Misses;
            jint l3Misses = stats->l3Misses;
            jdouble l2CacheHitRatio = stats->l2CacheHitRatio;
            jdouble l3CacheHitRatio = stats->l3CacheHitRatio;
            env->CallStaticVoidMethod(cls, mid, sourceContext, offset+tid, l2CacheHitRatio, l2Misses, l3CacheHitRatio, l3Misses);
            env->DeleteLocalRef(sourceContext);
        }
    }
    #endif

	dumpArrayAddrRanges();
}

void SendKernelMemUsageStatsToJVM( JNIEnv* env ) {
    #ifndef __DELITE_CPP_STANDALONE__
    jclass cls = env->FindClass("ppl/delite/runtime/profiler/MemoryProfiler");
    jmethodID mid = env->GetStaticMethodID(cls, "addCppKernelMemUsageStats", "(Ljava/lang/String;J)V");

    if (mid == NULL) {
        std::cout << "Could not find method 'addCppKernelMemUsageStats'" << std::endl;
        return;
    }

    for (int32_t tid=0; tid < numCpp; tid++) {
  		std::map< std::string, uint64_t >* m = kernelToMemUsageMaps->at(tid);
  		std::map< std::string, uint64_t >::iterator it;
  		for (it = m->begin(); it != m->end(); it++) {
              jstring kernel = env->NewStringUTF(it->first.c_str());
  			jlong memUsage = it->second;
              env->CallStaticVoidMethod(cls, mid, kernel, memUsage);
              env->DeleteLocalRef(kernel);
  		}
    }
    #endif
}

#ifndef DELITE_NUM_CUDA

BufferedFileWriter::BufferedFileWriter(const char* fileName)
{
    fs.open(fileName);
}

void BufferedFileWriter::writeTimer(std::string kernel, long start, double duration, int32_t level, int32_t tid, bool isMultiLoop) {
  if (isMultiLoop) {
    fs << kernel << "_" << tid << "," << start << "," << duration << "," << level << std::endl;
  } else {
    fs << kernel << "," << start << "," << duration << "," << level << std::endl;
  }
}

void BufferedFileWriter::close() {
  fs.close();
}

#endif
