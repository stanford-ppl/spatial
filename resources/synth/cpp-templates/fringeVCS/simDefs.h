#ifndef __SIM_DEFS_H
#define __SIM_DEFS_H

#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>
#include "commonDefs.h"

// Simulation CMD and RESP file descriptors
#define SIM_CMD_FD    1000
#define SIM_RESP_FD   1001

// Simulation commands
enum SIM_CMD { RESET, READY, START, STEP, GET_CYCLES, WRITE_REG, READ_REG, MALLOC, MEMCPY_H2D, MEMCPY_D2H, FREE, FIN };

const uint64_t maxSimCmdDataSize = 1024;
struct simCmd {
  int id;
  SIM_CMD cmd;
  uint8_t data[maxSimCmdDataSize];
  uint64_t size;
};

typedef struct simCmd simCmd;

void printPkt(simCmd *cmd) {
  EPRINTF("----- printPkt -----\n");
  EPRINTF("ID   : %d\n", cmd->id);
  EPRINTF("CMD  : %d\n", cmd->cmd);
  EPRINTF("SIZE : %lu\n", cmd->size);
  EPRINTF("----- End printPkt -----\n");
}

#endif // __SIM_DEFS_H
