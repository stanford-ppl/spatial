#ifndef __FRINGE_CONTEXT_SIM_H__
#define __FRINGE_CONTEXT_SIM_H__

#include "DUT.h"
#include "DUTTester.h"
#include "FringeContextBase.h"
#include "PeekPokeTester.h"
#include "verilated.h"
#if VM_TRACE
#include "verilated_vcd_c.h"
#endif
//#include <cstdlib>
#include <cstring>
#include <stdlib.h>
/**
 * Simulation Fringe Context
 */
class FringeContextSim : public FringeContextBase<DUT> {

  const uint32_t burstSizeBytes = 64;

public:
  PeekPokeTester *tester;
  std::string vcdfile;
  VerilatedVcdC *tfp = NULL;
  uint32_t numArgIns = 0;
  uint32_t numArgOuts = 0;

  FringeContextSim(std::string path = "") : FringeContextBase(path) {

    dut = new DUT;
    vcdfile = "DUT.vcd";

#if VM_TRACE
    Verilated::traceEverOn(true);
    VL_PRINTF("Enabling waves..\n");

    tfp = new VerilatedVcdC;
    dut->trace(tfp, 99);
    tfp->open(vcdfile.c_str());
#endif
    tester = new DUTTester(dut, tfp);
  }

  virtual void load() {
    for (int i=0; i<5; i++) {
      tester->reset();
    }
    tester->start();
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

  void flushCache(uint32_t mb) {
    // Iterate through an array the size of the L2$, to "flush" the cache aka fill it with garbage
    int cacheSizeWords = mb * (1 << 10) / sizeof(int); // 512kB on Zynq, 1MB on ZCU
    int arraySize = cacheSizeWords * 10;
    int *dummyBuf = (int*) std::malloc(arraySize * sizeof(int));
    EPRINTF("[memcpy] dummyBuf = %p, (phys = %lx), arraySize = %d\n", dummyBuf, getFPGAPhys((uint64_t) dummyBuf), arraySize);
    for (int i = 0; i<arraySize; i++) {
      if (i == 0) {
        dummyBuf[i] = 10;
      } else {
        dummyBuf[i] = dummyBuf[i-1] * 2;
      }
    }
    EPRINTF("[memcpy] dummyBuf = %p, dummyBuf[%d] = %d\n", dummyBuf, arraySize-1, dummyBuf[arraySize-1]);
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

  virtual void run() {
    tester->test();
  }

  virtual void setArg(uint32_t arg, uint64_t data, bool isIO) {
    writeReg(arg+2, data);
    numArgIns++;
  }

  virtual uint64_t getArg(uint32_t arg, bool isIO) {
    readReg(numArgIns+2+arg);
    numArgOuts++;

  }

  virtual void writeReg(uint32_t reg, uint64_t data) {
    tester->writeReg(reg, data);
  }

  virtual uint64_t readReg(uint32_t reg) {
    return tester->readReg(reg);
  }

  ~FringeContextSim() {
    tester->finish();

#if VM_TRACE
    if (tfp) tfp->close();
    delete tfp;
#endif
//    delete tester;
//    delete dut;
  }
};

// Fringe Simulation APIs
void fringeInit(int argc, char **argv) {
  Verilated::commandArgs(argc, argv);
}
#endif
