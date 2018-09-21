#ifndef __DUTTESTER_H__
#define  __DUTTESTER_H__
#include "PeekPokeTester.h"
#include "Callbacks.h"

/**
 * C++ tester for Top module with Fringe and Accel
 * The Fringe simulation API relies on the existence of a 'DUTTester' C++ PeekPokeTester.
 * To use the Fringe API, it is necessary to declare a DUTTester
 */
class DUTTester: public PeekPokeTester {
public:

  // Some constants for Top and Fringe
  const uint32_t commandReg = 0;
  const uint32_t statusReg = 1;
  const uint64_t maxCycles = 50000000;

  DUTTester(DUT* _dut, VerilatedVcdC *_tfp = NULL) : PeekPokeTester(_dut, _tfp) {
    watchMap[&(dut->io_dram_cmd_valid)] = &handleDRAMRequest;

    // Simulation DRAM is always ready
    poke(&(dut->io_dram_cmd_ready), 1);
  }

  virtual void writeReg(uint32_t reg, uint64_t data) {
   poke(&(dut->io_waddr), reg);
   poke(&(dut->io_wdata), data);
   poke(&(dut->io_wen), 1);
   step(1);
   poke(&(dut->io_wen), 0);
  }

  // Register reads are modeled to not be combinational
  // so that they mimic real-world functionality, and they
  // can be seen on the VCD waveform
  // This can be revisited if simulation takes a long time
  // for big designs
  virtual uint64_t readReg(uint32_t reg) {
   poke(&(dut->io_raddr), reg);
   step(1);
   return peek(&(dut->io_rdata));
  }

  // This called by the 'run' method
  virtual void test() {

    // Current assumption is that the design sets arguments individually
    uint32_t status = 0;

    // Implement 4-way handshake
    writeReg(statusReg, 0);
    writeReg(commandReg, 1);
    numCycles = 0;  // restart cycle count (incremented with each step())
    while((status == 0) && (numCycles <= maxCycles)) {
      step();
      status = readReg(statusReg);
    }
    finishSim();
    std::cout << "Design ran for " << numCycles << " cycles" << std::endl;
    if (numCycles > maxCycles) { // Design did not run to completion
      std::cout << "=========================================\nERROR: Simulation terminated after " << maxCycles << " cycles\n=========================================" << std::endl;
    } else {  // Ran to completion, pull down command signal
      writeReg(commandReg, 0);
      while (status == 1) {
        step();
        status = readReg(statusReg);
      }
    }
  }

  virtual void executeEveryCycle() {
    dramResponse(dut, this);
  }
  virtual void finishSim() {
    drainQueue(dut, this);
  }

};

#endif
