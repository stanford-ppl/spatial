#ifndef __DRAM_DEFS_H
#define __DRAM_DEFS_H

#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>
#include "commonDefs.h"

// Simulation CMD and RESP file descriptors
#define DRAM_CMD_FD    1000
#define DRAM_RESP_FD   1001

// Simulation commands
enum DRAM_CMD {
  DRAM_READY,
  DRAM_STEP,
  DRAM_MALLOC,
  DRAM_MEMCPY_H2D,
  DRAM_MEMCPY_D2H,
  DRAM_FREE,
  DRAM_ENQ_TX,
  DRAM_GET_FINISHED_TX,
  DRAM_FIN
};

const uint64_t maxDRAMCmdDataSize = 1024;
struct dramCmd {
  int id;
  DRAM_CMD cmd;
  uint8_t data[maxDRAMCmdDataSize];
  uint64_t size;
};

typedef struct dramCmd dramCmd;

void printPkt(dramCmd *cmd) {
  EPRINTF("----- printPkt -----\n");
  EPRINTF("ID   : %d\n", cmd->id);
  EPRINTF("CMD  : %d\n", cmd->cmd);
  EPRINTF("SIZE : %lu\n", cmd->size);
  EPRINTF("----- End printPkt -----\n");
}

#endif // __DRAM_DEFS_H
