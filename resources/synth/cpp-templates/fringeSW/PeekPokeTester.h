#ifndef __PEEK_POKE_TESTER_H__
#define __PEEK_POKE_TESTER_H__

#include "DUT.h"
#include "verilated.h"
#include "verilated_vcd_c.h"
#include <iostream>
#include <map>

class PeekPokeTester {
typedef void (*CallbackFunction)(DUT*, PeekPokeTester*);
typedef std::map<CData*, CallbackFunction> signalCallbackMap;
private:
    bool is_exit;

protected:
  DUT* dut;
  vluint64_t main_time;
  uint64_t numCycles;
  VerilatedVcdC* tfp;
  signalCallbackMap watchMap; 

public:
    PeekPokeTester(DUT* _dut, VerilatedVcdC *_tfp = NULL) {
        dut = _dut;
        tfp = _tfp;
        main_time = 0L;
        is_exit = false;
    }

    void init_dump(VerilatedVcdC* _tfp) { tfp = _tfp; }
    inline bool exit() { return is_exit; }
    virtual inline double get_time_stamp() {
        return main_time;
    }

    // Following methods return without doing any useful work
    void init_channels() { }

    void reset() {
        dut->reset = 1;
        step();
        numCycles = 0;
    }
    void start() {
        dut->reset = 0;
        dut->eval();
        numCycles = 0;
    }
    void finish() {
        // Do a 'half step' before returning, so that
        // updated values are visible on waveform
        dut->clock = 1;
        dut->eval();
        if (tfp) tfp->dump(++main_time);
        tfp->flush();
        is_exit = true;
    }

    void step() {
        // Set all poke/peek values on leading edge
        dut->clock = 1;
        dut->eval();
        if (tfp) tfp->dump(main_time);
        main_time++;

        dut->clock = 0;
        dut->eval();
        if (tfp) tfp->dump(main_time);
        main_time++;

        // Eval again on leading edge of current clock cycle
        // so that 'peek' called after 'step' returns
        // the stored values in state elements like FFs
        // Do not advance main_time, as we have already done so above
        dut->clock = 1;
        dut->eval();
        if (tfp) tfp->dump(main_time);
        numCycles++;

        // Flush after certain number of cycles
        if (numCycles % 10 == 0) {
          tfp->flush();
        }

        // Some functions (e.g. monitor DRAM queue, send DRAM response)
        // needs to be executed every cycle
        if (numCycles % 107 == 0) {
          executeEveryCycle();  
        } else {
          this->poke(&(dut->io_dram_resp_valid), 0);

        }

        // Handle callbacks for registered signals being watched
        watch();
    }

    void step(int n) {
      for (int i=0; i<n; i++) {
        step();
      }
    }
    void update() {
        dut->_eval_settle(dut->__VlSymsp);
        if (tfp) tfp->dump(main_time);
    }

    void run() {
      reset();
      reset();
      reset();
      reset();
      start();
      test();
      finish();
    }

    void tick() {
      run();
    }

    // Meat of the testing API
    void poke(CData *signal, uint8_t value) {
      *signal = value;
    }

    uint8_t peek(CData *signal) {
      update();
      return *signal;
    }

    void poke(SData *signal, uint16_t value) {
      *signal = value;
    }

    uint16_t peek(SData *signal) {
      update();
      return *signal;
    }

    void poke(IData *signal, uint32_t value) {
      *signal = value;
    }

    uint32_t peek(IData *signal) {
      update();
      return *signal;
    }

    void poke(QData *signal, uint64_t value) {
      *signal = value;
    }

    uint64_t peek(QData *signal) {
      update();
      return *signal;
    }

    virtual void writeReg(uint32_t reg, uint64_t data) {}
    virtual uint64_t readReg(uint32_t reg) { return 0;}
    virtual void test() = 0;

    virtual void watch() {
      // Iterate through all keys, peek them
      // Call the callbacks if the peek returns a non-zero value
      for (signalCallbackMap::iterator i = watchMap.begin(); i != watchMap.end(); i++) {
         CData *signal = i->first;
         CData value = peek(signal);
         if (value != 0) {
           (i->second)(dut, this);
         }
      }
    }

    virtual void executeEveryCycle() {

    }
    virtual void finishSim() {

    }

};
#endif
