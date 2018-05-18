#ifndef __CALLBACKS_H__
#define __CALLBACKS_H__

#include "DUT.h"
#include "PeekPokeTester.h"
#include <queue>

class DRAMRequest {
public:
  uint64_t addr;
  uint64_t tag;
  bool isWr;
  uint32_t *wdata;

  DRAMRequest(uint64_t a, uint64_t t, bool wr, uint32_t *wd) {
    addr = a;
    tag = t;
    isWr = wr;
    if (isWr) {
      wdata = (uint32_t*) malloc(16 * sizeof(uint32_t));
      for (int i=0; i<16; i++) {
        wdata[i] = wd[i];
      }
    } else {
      wdata = NULL;
    }
  }

  ~DRAMRequest() {
    if (wdata != NULL) free(wdata);
  }
};

std::queue<DRAMRequest*> dramRequestQ;

// Callback function when a valid DRAM request is seen
void handleDRAMRequest(DUT *dut, PeekPokeTester *tester) {
  std::cout << "DRAM request detected:" << std::endl;
  uint64_t addr = tester->peek(&(dut->io_dram_cmd_bits_addr));
  uint64_t tag = tester->peek(&(dut->io_dram_cmd_bits_tag));
  bool isWr = (tester->peek(&(dut->io_dram_cmd_bits_isWr)) > 0);
  printf("addr: %x, tag: %x, isWr: %u \n", addr, tag, isWr);
  uint32_t *wdata = NULL;
  if (isWr) {
    wdata = (uint32_t*) malloc(16 * sizeof(uint32_t));
    wdata[0] = tester->peek(&(dut->io_dram_cmd_bits_wdata_0));
    wdata[1] = tester->peek(&(dut->io_dram_cmd_bits_wdata_1));
    wdata[2] = tester->peek(&(dut->io_dram_cmd_bits_wdata_2));
    wdata[3] = tester->peek(&(dut->io_dram_cmd_bits_wdata_3));
    wdata[4] = tester->peek(&(dut->io_dram_cmd_bits_wdata_4));
    wdata[5] = tester->peek(&(dut->io_dram_cmd_bits_wdata_5));
    wdata[6] = tester->peek(&(dut->io_dram_cmd_bits_wdata_6));
    wdata[7] = tester->peek(&(dut->io_dram_cmd_bits_wdata_7));
    wdata[8] = tester->peek(&(dut->io_dram_cmd_bits_wdata_8));
    wdata[9] = tester->peek(&(dut->io_dram_cmd_bits_wdata_9));
    wdata[10] = tester->peek(&(dut->io_dram_cmd_bits_wdata_10));
    wdata[11] = tester->peek(&(dut->io_dram_cmd_bits_wdata_11));
    wdata[12] = tester->peek(&(dut->io_dram_cmd_bits_wdata_12));
    wdata[13] = tester->peek(&(dut->io_dram_cmd_bits_wdata_13));
    wdata[14] = tester->peek(&(dut->io_dram_cmd_bits_wdata_14));
    wdata[15] = tester->peek(&(dut->io_dram_cmd_bits_wdata_15));
  }

  dramRequestQ.push(new DRAMRequest(addr, tag, isWr, wdata));
}

void dramResponse(DUT *dut, PeekPokeTester *tester) {
  if (dramRequestQ.size() > 0) {
    DRAMRequest *req = dramRequestQ.front();
    dramRequestQ.pop();
    if (req->isWr) {
      // Write request: Update 1 burst-length bytes at *addr
      uint32_t *waddr = (uint32_t*) req->addr;
      for (int i=0; i<16; i++) {
        waddr[i] = req->wdata[i];
      }
    } else {
      // Read request: Read burst-length bytes at *addr
      uint32_t *raddr = (uint32_t*) req->addr;
      // std::cout << raddr[0] << std::endl;
      // std::cout << raddr[1] << std::endl;
      // std::cout << raddr[2] << std::endl;
      // std::cout << raddr[3] << std::endl;
      // std::cout << raddr[4] << std::endl;
      // std::cout << raddr[5] << std::endl;
      // std::cout << raddr[6] << std::endl;
      // std::cout << raddr[7] << std::endl;
      // std::cout << raddr[8] << std::endl;
      // std::cout << raddr[9] << std::endl;
      // std::cout << raddr[10] << std::endl;
      // std::cout << raddr[11] << std::endl;
      // std::cout << raddr[12] << std::endl;
      // std::cout << raddr[13] << std::endl;
      // std::cout << raddr[14] << std::endl;
      // std::cout << raddr[15] << std::endl;
      tester->poke(&(dut->io_dram_resp_bits_rdata_0), raddr[0]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_1), raddr[1]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_2), raddr[2]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_3), raddr[3]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_4), raddr[4]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_5), raddr[5]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_6), raddr[6]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_7), raddr[7]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_8), raddr[8]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_9), raddr[9]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_10), raddr[10]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_11), raddr[11]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_12), raddr[12]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_13), raddr[13]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_14), raddr[14]);
      tester->poke(&(dut->io_dram_resp_bits_rdata_15), raddr[15]);
    }

    tester->poke(&(dut->io_dram_resp_valid), 1);
    tester->poke(&(dut->io_dram_resp_bits_tag), req->tag);
  } else {
    tester->poke(&(dut->io_dram_resp_valid), 0);
  }
}

void drainQueue(DUT *dut, PeekPokeTester *tester) {
  while (dramRequestQ.size() > 0) {
    dramResponse(dut, tester);
  }
}
#endif
