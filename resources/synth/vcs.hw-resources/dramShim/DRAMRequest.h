#ifndef __DRAMREQUEST_H
#define __DRAMREQUEST_H

#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>
#include "commonDefs.h"

class DRAMRequest {
public:
  uint64_t addr;
  uint64_t tag;
  bool isWr;
  uint32_t *wdata;
  uint32_t delay;
  uint32_t elapsed;

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

    delay = abs(rand()) % 150 + 50;
    elapsed = 0;
  }

  void print() {
    EPRINTF("---- DRAM REQ ----\n");
    EPRINTF("addr : %lx\n", addr);
    EPRINTF("tag  : %lx\n", tag);
    EPRINTF("isWr : %d\n", isWr);
    EPRINTF("delay: %u\n", delay);
    if (isWr) {
      EPRINTF("wdata0 : %u\n", wdata[0]);
      EPRINTF("wdata1 : %u\n", wdata[1]);
    }
    EPRINTF("------------------\n");
  }

  ~DRAMRequest() {
    if (wdata != NULL) free(wdata);
  }
};



#endif // __DRAMREQUEST_H
