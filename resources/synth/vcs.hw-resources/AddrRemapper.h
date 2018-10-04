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

#include "commonDefs.h"
#include "vc_hdrs.h"
#include "svdpi_src.h"

using namespace std;

class AddrRemapper {
public:

  const uint32_t pageSize = 4096;  // Bytes
  map<uint32_t, uint64_t> addrMap;
  uint32_t nextAvailAddr = 4096; // Next available address

  AddrRemapper() {
  }

  uint32_t getNumPages(size_t size) {
    if (size < pageSize) {
      return 1;
    } else {
      return (size / pageSize);
    }
  }

  size_t alignedSize(uint32_t alignment, size_t size) {
    if ((size % alignment) == 0) {
      return size;
    } else {
      return size + alignment - (size % alignment);
    }
  }

  uint32_t getPageBase(uint32_t addr) {
    uint32_t pageNum = addr / pageSize;
    return pageNum * pageSize;
  }

  uint32_t getPageOffset(uint32_t addr) {
    // 64-bit and 32-bit addresses both have the same page offset
    return addr % pageSize;
  }

  uint32_t remap(uint64_t ptr, size_t size) {
    uint32_t addr = nextAvailAddr;
    uint32_t sizeInPages = getNumPages(alignedSize(pageSize, size));
    nextAvailAddr += sizeInPages * pageSize;

    // Create entries in hashmap
    for (uint32_t i = 0; i < sizeInPages; i++) {
      uint32_t smallAddr = addr + pageSize * i;
      uint64_t bigAddr = ptr + pageSize * i;
      addrMap[smallAddr] = bigAddr;
    }
    return addr;
  }

  uint64_t getBig(uint64_t smallAddr) {
    uint32_t pageBase = getPageBase((uint32_t)smallAddr);
    uint32_t pageOffset = getPageOffset((uint32_t)smallAddr);

    map<uint32_t, uint64_t>::iterator it = addrMap.find(pageBase);
    if(it == addrMap.end()) {
      EPRINTF("[AddrRemapper getBig] Address %x does not exist in addrMap!\n", (uint32_t)smallAddr);
      terminateSim();
    }
    return it->second + pageOffset;
  }

  ~AddrRemapper() {
    addrMap.clear();
  }
};
