#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <vector>
#include <queue>
#include <poll.h>
#include <fcntl.h>
#include <sys/mman.h>

using namespace std;

#include "DRAMRequest.h"
#include "dramDefs.h"
#include "channel.h"

Channel *cmdChannel = NULL;
Channel *respChannel = NULL;

int sendResp(dramCmd *cmd) {
  dramCmd resp;
  resp.id = cmd->id;
  resp.cmd = cmd->cmd;
  resp.size = cmd->size;
  switch (cmd->cmd) {
    case DRAM_READY:
      resp.size = 0;
      break;
    default:
      EPRINTF("[SIM] Command %d not supported!\n", cmd->cmd);
      exit(-1);
  }

  respChannel->send(&resp);
  return cmd->id;
}

int numCycles = 0;

std::queue<DRAMRequest*> dramRequestQ;

void recvDRAMRequest(
    uint64_t addr,
    uint64_t tag,
    bool isWr,
    uint32_t *wdata
  ) {

  DRAMRequest *req = new DRAMRequest(addr, tag, isWr, wdata);
  dramRequestQ.push(req);
  req->print();
}

void checkDRAMResponse() {
  if (dramRequestQ.size() > 0) {
    DRAMRequest *req = dramRequestQ.front();
    req->elapsed++;
    if(req->elapsed == req->delay) {
      dramRequestQ.pop();

      uint32_t rdata[16] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

      if (req->isWr) {
        // Write request: Update 1 burst-length bytes at *addr
        uint32_t *waddr = (uint32_t*) req->addr;
        for (int i=0; i<16; i++) {
          waddr[i] = req->wdata[i];
        }
      } else {
        // Read request: Read burst-length bytes at *addr
        uint32_t *raddr = (uint32_t*) req->addr;
        for (int i=0; i<16; i++) {
          rdata[i] = raddr[i];
        }

      }
    }
  }
}

// Function is called every clock cycle
int tick() {
  bool exitTick = false;
  int finishSim = 0;
  numCycles++;

 // Check for DRAM response and send it to design
 checkDRAMResponse();

  // Handle new incoming operations
  while (!exitTick) {
    dramCmd *cmd = (dramCmd*) cmdChannel->recv();
    dramCmd readResp;
    switch (cmd->cmd) {
      case DRAM_MALLOC: {
        size_t size = *(size_t*)cmd->data;
        int fd = open("/dev/zero", O_RDWR);
        void *ptr = mmap(0, size, PROT_READ|PROT_WRITE, MAP_PRIVATE, fd, 0);
        close(fd);

        dramCmd resp;
        resp.id = cmd->id;
        resp.cmd = cmd->cmd;
        *(uint64_t*)resp.data = (uint64_t)ptr;
        resp.size = sizeof(size_t);
        EPRINTF("[SIM] MALLOC(%lu), returning %p\n", size, (void*)ptr);
        respChannel->send(&resp);
        break;
      }
      case DRAM_FREE: {
        void *ptr = (void*)(*(uint64_t*)cmd->data);
        ASSERT(ptr != NULL, "Attempting to call free on null pointer\n");
        EPRINTF("[SIM] FREE(%p)\n", ptr);
        break;
      }
      case DRAM_MEMCPY_H2D: {
        uint64_t *data = (uint64_t*)cmd->data;
        void *dst = (void*)data[0];
        size_t size = data[1];

        EPRINTF("[SIM] Received memcpy request to %p, size %lu\n", (void*)dst, size);

        // Now to receive 'size' bytes from the cmd stream
        cmdChannel->recvFixedBytes(dst, size);

        // Send ack back indicating end of memcpy
        dramCmd resp;
        resp.id = cmd->id;
        resp.cmd = cmd->cmd;
        resp.size = 0;
        respChannel->send(&resp);
        break;
      }
      case DRAM_MEMCPY_D2H: {
        // Transfer 'size' bytes from src
        uint64_t *data = (uint64_t*)cmd->data;
        void *src = (void*)data[0];
        size_t size = data[1];

        // Now to receive 'size' bytes from the cmd stream
        respChannel->sendFixedBytes(src, size);
        break;
      }
      case DRAM_STEP:
        exitTick = true;
        break;
      case DRAM_FIN:
        finishSim = 1;
        exitTick = true;
        break;
      default:
        break;
    }
  }
  return finishSim;
}

void dramLoop() {
  while (!tick());
}

// Called before simulation begins
int main(int argc, char **argv) {
  EPRINTF("[DRAM] DRAM process started!\n");

  // 0. Create Channel structures
  cmdChannel = new Channel(DRAM_CMD_FD, -1, sizeof(dramCmd));
  respChannel = new Channel(-1, DRAM_RESP_FD, sizeof(dramCmd));

  // 1. Read command
  dramCmd *cmd = (dramCmd*) cmdChannel->recv();

  // 2. Send response
  sendResp(cmd);

  // 3. Go into the DRAM loop
  dramLoop();

  EPRINTF("[DRAM] Quitting DRAM simulator\n");

  return 0;
}
