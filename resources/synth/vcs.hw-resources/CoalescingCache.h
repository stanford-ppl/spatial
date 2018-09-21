#include <unistd.h>
#include <errno.h>
#include <cstring>
#include <string>
#include <cstdlib>
#include <stdio.h>
#include <vector>
#include <queue>
#include <map>
#include <poll.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/prctl.h>
using namespace std;

#include "vc_hdrs.h"
#include "svdpi_src.h"

#include <DRAMSim.h>

class CoalescingCache {
public:

  uint64_t maxLines = 0;
  std::map<uint64_t, uint32_t*> cache;

  CoalescingCache(uint64_t lines) {
    maxLines = lines;
  }

  bool isFull() {
    return (cache.size == maxLines);
  }

  bool isEmpty() {
    return (cache.size == 0);
  }

  void write(uint64_t addr) {

  }

  void readEvict(uint64_t addr) {

  }


  ~CoalescingCache() {

  }
};
