#ifndef __FRINGE_CONTEXT_ZYNQ_H__
#define __FRINGE_CONTEXT_ZYNQ_H__

#include "FringeContextBase.h"
#include "ZynqAddressMap.h"
#include "ZynqUtils.h"
#include <cstring>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/mman.h>

/**
 * Simulation Fringe Context
 */
class FringeContextZynq : public FringeContextBase<void> {

  const uint32_t burstSizeBytes = 64;
  int fd = 0;
  u32 fringeScalarBase = 0;
  const u32 commandReg = 0;
  const u32 statusReg = 1;
public:
  uint32_t numArgIns = 0;
  uint32_t numArgOuts = 0;
  std::string bitfile = "";

  FringeContextZynq(std::string path = "") : FringeContextBase(path) {
    bitfile = path;

    // open /dev/mem file
    int retval = setuid(0);
    ASSERT(retval == 0, "setuid(0) failed\n");
    fd = open ("/dev/mem", O_RDWR);
    if (fd < 1) {
      perror("error opening /dev/mem\n");
    }

    // Initialize pointers to fringeScalarBase
    void *ptr;
    ptr = mmap(NULL, MAP_LEN, PROT_READ|PROT_WRITE, MAP_SHARED, fd, FRINGE_SCALAR_BASEADDR);
    fringeScalarBase = (u32) ptr;
  }

  virtual void load() {
    std::string cmd = "prog_fpga " + bitfile;
    system(cmd.c_str());
  }

  size_t alignedSize(uint32_t alignment, size_t size) {
    if ((size % alignment) == 0) {
      return size;
    } else {
      return size + alignment - (size % alignment);
    }
  }
  virtual uint64_t malloc(size_t bytes) {
    size_t paddedSize = alignedSize(burstSizeBytes, bytes);
    void *ptr = aligned_alloc(burstSizeBytes, paddedSize);
    return (uint64_t) ptr;
  }

  virtual void free(uint64_t buf) {
    std::free((void*) buf);
  }

  virtual void memcpy(uint64_t devmem, void* hostmem, size_t size) {
    std::memcpy((void*)devmem, hostmem, size);
  }

  virtual void memcpy(void* hostmem, uint64_t devmem, size_t size) {
    std::memcpy(hostmem, (void*)devmem, size);
  }

  void dumpRegs() {
    fprintf(stderr, "---- DUMPREGS ----\n");
    for (int i=0; i<4; i++) {
      fprintf(stderr, "reg[%d] = %u\n", i, readReg(i));
    }
    fprintf(stderr, "---- END DUMPREGS ----\n");
  }
  virtual void run() {
     // Current assumption is that the design sets arguments individually
    uint32_t status = 0;

    // Implement 4-way handshake
    writeReg(statusReg, 0);
    writeReg(commandReg, 1);

    fprintf(stderr, "Running design..\n");
    double startTime = getTime();
    while((status == 0)) {
      status = readReg(statusReg);
    }
    double endTime = getTime();
    fprintf(stderr, "Design done, ran for %lf secs\n", endTime - startTime);
    writeReg(commandReg, 0);
    while (status == 1) {
      status = readReg(statusReg);
    }
  }

  virtual void setArg(uint32_t arg, uint64_t data) {
    writeReg(arg+2, data);
    numArgIns++;
  }

  virtual uint64_t getArg(uint32_t arg) {
    numArgOuts++;
    return readReg(numArgIns+2+arg);

  }

  virtual void writeReg(uint32_t reg, uint64_t data) {
    Xil_Out32(fringeScalarBase+reg*sizeof(u32), data);
  }

  virtual uint64_t readReg(uint32_t reg) {
    return Xil_In32(fringeScalarBase+reg*sizeof(u32));
  }

  ~FringeContextZynq() {
  }
};

// Fringe Simulation APIs
void fringeInit(int argc, char **argv) {
}
#endif
