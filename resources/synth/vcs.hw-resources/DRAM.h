#include <unistd.h>
#include <errno.h>
#include <cstring>
#include <string>
#include <cstdlib>
#include <stdio.h>
#include <vector>
#include <deque>
#include <map>
#include <poll.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/prctl.h>
using namespace std;

#include "vc_hdrs.h"
#include "svdpi_src.h"

#include <AddrRemapper.h>
#include <DRAMSim.h>

#define MAX_NUM_Q             128
#define PAGE_SIZE_BYTES       4096
#define PAGE_OFFSET           (__builtin_ctz(PAGE_SIZE_BYTES))
#define PAGE_FRAME_NUM(addr)  (addr >> PAGE_OFFSET)

// N3XT constants
uint32_t N3XT_LOAD_DELAY = 3;
uint32_t N3XT_STORE_DELAY = 11;
uint32_t N3XT_NUM_CHANNELS = MAX_NUM_Q;

// Simulation constants
FILE *traceFp = NULL;

// DRAMSim2
DRAMSim::MultiChannelMemorySystem *mem = NULL;
bool useIdealDRAM = false;
bool debug = false;
AddrRemapper *remapper = NULL;

extern uint64_t numCycles;
uint32_t wordSizeBytes = 1;
uint32_t burstSizeBytes = 64;
uint32_t burstSizeWords = burstSizeBytes / wordSizeBytes;

uint64_t globalID = 0;

class DRAMRequest;
class WData;

typedef union DRAMTag {
  struct {
    unsigned int uid : 32;
    unsigned int streamId : 32;
  };
  uint64_t tag;
} DRAMTag;

/**
 * DRAM Command received from the design
 * One object could potentially create multiple DRAMRequests
 */
class DRAMCommand {
public:
  uint64_t addr;
  uint32_t size;
  DRAMTag tag;
  uint64_t channelID;
  bool isWr;
  DRAMRequest **reqs = NULL;

  DRAMCommand(uint64_t a, uint32_t sz, DRAMTag t, bool wr) {
    addr = a;
    size = sz;
    tag = t;
    isWr = wr;
  }

  bool hasCompleted(); // Method definition after 'DRAMRequest' class due to forward reference

  ~DRAMCommand() {
    if (reqs != NULL) free(reqs);
  }
};


// DRAM Request Queue
std::deque<DRAMRequest*> dramRequestQ[MAX_NUM_Q];

class WData {
public:
  uint8_t *wdata = NULL;
  uint8_t *wstrb = NULL;

  WData() {
    wdata = NULL;
    wstrb = NULL;
  }

  void print() {
  }

  ~WData() {
    if (wdata != NULL) free(wdata);
    if (wstrb != NULL) free(wstrb);
  }

};

/**
 * DRAM Request corresponding to 1-burst that is enqueued into DRAMSim2
 * The 'size' field reflects the size of the entire command of which
 * this request is part of (legacy, should be removed)
 */
class DRAMRequest {
public:
  uint64_t id;
  uint64_t addr;
  uint64_t rawAddr;
  uint64_t smallAddr;
  uint32_t size;
  DRAMTag tag;
  uint64_t channelID;
  bool isWr;
  uint8_t *wdata = NULL;
  uint32_t delay;
  uint32_t elapsed;
  uint64_t issued;
  bool completed;
  DRAMCommand *cmd;

  DRAMRequest(uint64_t a, uint64_t ra, uint32_t sz, DRAMTag t, bool wr, uint64_t issueCycle) {
    id = globalID++;
    addr = remapper->getBig(a);
    smallAddr = a;
    rawAddr = ra;
    size = sz;
    tag = t;
    isWr = wr;

    // N3xt delay
    if (isWr) {
      delay = N3XT_STORE_DELAY;
    } else {
      delay = N3XT_LOAD_DELAY;
    }
    if (useIdealDRAM) {
      channelID = id % N3XT_NUM_CHANNELS;
    }

    elapsed = 0;
    issued = issueCycle;
    completed = false;
    wdata = NULL;
  }

  void print() {
    EPRINTF("[DRAMRequest CH=%lu] addr: %lx (%lx), sizeInBursts: %u, streamId: %x, tag: %lx, isWr: %d, issued=%lu \n", channelID, addr, rawAddr, size, tag.streamId, tag.uid, isWr, issued);
  }

  void schedule() {
    if (!useIdealDRAM) {
      mem->addTransaction(isWr, addr, tag.tag);
      channelID = mem->findChannelNumber(addr);
    } else {
      dramRequestQ[channelID].push_back(this);
      ASSERT(channelID < MAX_NUM_Q, "channelID %d is greater than MAX_NUM_Q %u. Is N3XT_NUM_CHANNELS (%u) > MAX_NUM_Q (%u) ?\n", channelID, MAX_NUM_Q, MAX_NUM_Q, N3XT_NUM_CHANNELS);
    }
    if (debug) {
      EPRINTF("                  Issuing following command:");
      print();
    }

  }

  ~DRAMRequest() {
    if (wdata != NULL) free(wdata);
  }
};

bool DRAMCommand::hasCompleted() {
  bool completed = true;
  for (int i = 0; i < size; i++) {
    if (!reqs[i]->completed) {
      completed = false;
      break;
    }
  }
  return completed;
}


struct AddrTag {
  uint64_t addr;
  DRAMTag tag;

  AddrTag(uint64_t a, DRAMTag t) {
    addr = a;
    tag = t;
  }

  bool operator==(const AddrTag &o) const {
      return addr == o.addr && tag.tag == o.tag.tag;
  }

  bool operator<(const AddrTag &o) const {
      return addr < o.addr || (addr == o.addr && tag.tag < o.tag.tag);
  }
};


// WDATA Queue - Should really only hold data
std::deque<WData*> wdataQ; 

// Current set of requests that will be getting their wdata in order
std::deque<DRAMRequest*> wrequestQ;

// Internal book-keeping data structures
std::map<struct AddrTag, DRAMRequest*> addrToReqMap;

uint32_t getWordOffset(uint64_t addr) {
  return (addr & (burstSizeBytes - 1)) >> 2;   // TODO: Use parameters above!
}

void printQueueStats(int id) {
  // Ensure that all top 16 requests have been completed
  if (dramRequestQ[id].size() > 0) {
    deque<DRAMRequest*>::iterator it = dramRequestQ[id].begin();

    EPRINTF("==== dramRequestQ %d status =====\n", id);
    int k = 0;
    while (it != dramRequestQ[id].end()) {
      DRAMRequest *r = *it;
      EPRINTF("    %d. addr: %lx (%lx), tag: %lx, streamId: %d, completed: %d\n", k, r->addr, r->rawAddr, r->tag.uid, r->tag.streamId, r->completed);
      it++;
      k++;
      //    if (k > 20) break;
    }
    EPRINTF("==== END dramRequestQ %d status =====\n", id);

  }
}

void updateIdealDRAMQ(int id) {
  if (dramRequestQ[id].size() > 0) {
    DRAMRequest *req = dramRequestQ[id].front();

    if (useIdealDRAM) {
      req->elapsed++;
      if (req->elapsed >= req->delay) {
        req->completed = true;
        if (debug) {
          EPRINTF("[idealDRAM txComplete] addr = %p, tag = %lx, finished = %lu\n", (void*)req->addr, req->tag.uid, numCycles);
        }

      }
    }
  }
}

int popWhenReady = -1; // dramRequestQ from which response was poked (to be popped when ready)

/**
 * DRAM Queue pop: Called from testbench when response ready & valid is high
 * Both read and write requests are in a single queue in the simulation
 * So both 'popDRAMReadQ' and 'popDRAMWriteQ' call the same internal function
 * The separation in the DPI API is for future-proofing when we truly support
 * independent read and write DRAM channel simulation
 */
void popDRAMQ() {
  ASSERT(popWhenReady != -1, "popWhenReady == -1! Popping before the first command was issued?\n");
  ASSERT(popWhenReady < MAX_NUM_Q, "popWhenReady = %d which is greater than MAX_NUM_Q (%d)!\n", popWhenReady, MAX_NUM_Q);

  DRAMRequest *req = dramRequestQ[popWhenReady].front();
  ASSERT(req->completed, "Request at the head of pop queue (%d) not completed!\n", popWhenReady);
  ASSERT(req != NULL, "Request at head of pop queue (%d) is null!\n", req);

  if (req->isWr) { // Write request
    ASSERT(req->cmd->hasCompleted(), "Write command at head of pop queue (%d) is not fully complete!\n", popWhenReady);
    DRAMCommand *cmd = req->cmd;
    DRAMRequest *front = req;
    // Do write data handling, then pop all requests belonging to finished cmd from FIFO
    while ((dramRequestQ[popWhenReady].size() > 0) && (front->cmd == cmd)) {
      uint8_t *front_wdata = front->wdata;
      uint8_t *front_waddr = (uint8_t*) front->addr;
      for (int i=0; i<burstSizeWords; i++) {
        front_waddr[i] = front_wdata[i];
      }
      dramRequestQ[popWhenReady].pop_front();
      delete front;
      front = dramRequestQ[popWhenReady].front();
    }
    delete cmd;

  } else {  // Read request: Just pop
    dramRequestQ[popWhenReady].pop_front();
    // TODO: Uncommenting lines below is causing a segfault
    // More cleaner garbage collection required, but isn't an immediate problem
//          if (req->cmd->hasCompleted()) {
//            delete req->cmd;
//          }
    delete req;
  }

  // Reset popWhenReady
  popWhenReady = -1;
}

void popDRAMReadQ() {
  popDRAMQ();
}

void popDRAMWriteQ() {
  popDRAMQ();
}

bool checkQAndRespond(int id) {
  // If request at front has completed, pop and poke DRAM response
  bool pokedResponse = false;
  if (dramRequestQ[id].size() > 0) {
    DRAMRequest *req = dramRequestQ[id].front();

//    if (useIdealDRAM) {
//      req->elapsed++;
//      if (req->elapsed >= req->delay) {
//        req->completed = true;
//        if (debug) {
//          EPRINTF("[idealDRAM txComplete] addr = %p, tag = %lx, finished = %lu\n", (void*)req->addr, req->tag, numCycles);
//        }
//      }
//    }

    if (req->completed) {
      uint8_t rdata[64] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
      bool pokeResponse = false;

      if (req->isWr) { // Write request: Update 1 burst-length bytes at *addr
        uint8_t *wdata = req->wdata;
        uint8_t *waddr = (uint8_t*) req->addr;
        for (int i=0; i<burstSizeWords; i++) {
          waddr[i] = wdata[i];
        }
        if (req->cmd->hasCompleted()) {
          pokeResponse = true;
        }
      } else { // Read request: Read burst-length bytes at *addr
        uint8_t *raddr = (uint8_t*) req->addr;
        for (int i=0; i<burstSizeWords; i++) {
          rdata[i] = raddr[i];
        }
        pokeResponse = true;
      }

      if (pokeResponse) { // Send DRAM response
        if (debug) {
          EPRINTF("[Sending DRAM resp to]: ");
          req->print();
        }

      // N3Xt logging info
      fprintf(traceFp, "id: %lu\n", req->id);
      fprintf(traceFp, "issue: %lu\n", req->issued);
      if (req->isWr) {
        fprintf(traceFp, "type: STORE\n");
      } else {
        fprintf(traceFp, "type: LOAD\n");
      }
      fprintf(traceFp, "delay: %d\n", numCycles - req->issued);
      fprintf(traceFp, "addr: %lu\n", req->smallAddr);
      fprintf(traceFp, "size: %u\n", burstSizeBytes);
      fprintf(traceFp, "channel: %d\n", req->channelID);
      fprintf(traceFp, "\n");


        if (req->isWr) {
          pokeDRAMWriteResponse(req->tag.uid, req->tag.streamId);
        } else {
          pokeDRAMReadResponse(
            req->tag.uid,
            req->tag.streamId,
            rdata[0],
            rdata[1],
            rdata[2],
            rdata[3],
            rdata[4],
            rdata[5],
            rdata[6],
            rdata[7],
            rdata[8],
            rdata[9],
            rdata[10],
            rdata[11],
            rdata[12],
            rdata[13],
            rdata[14],
            rdata[15],
            rdata[16],
            rdata[17],
            rdata[18],
            rdata[19],
            rdata[20],
            rdata[21],
            rdata[22],
            rdata[23],
            rdata[24],
            rdata[25],
            rdata[26],
            rdata[27],
            rdata[28],
            rdata[29],
            rdata[30],
            rdata[31],
            rdata[32],
            rdata[33],
            rdata[34],
            rdata[35],
            rdata[36],
            rdata[37],
            rdata[38],
            rdata[39],
            rdata[40],
            rdata[41],
            rdata[42],
            rdata[43],
            rdata[44],
            rdata[45],
            rdata[46],
            rdata[47],
            rdata[48],
            rdata[49],
            rdata[50],
            rdata[51],
            rdata[52],
            rdata[53],
            rdata[54],
            rdata[55],
            rdata[56],
            rdata[57],
            rdata[58],
            rdata[59],
            rdata[60],
            rdata[61],
            rdata[62],
            rdata[63]

          );
        }
        pokedResponse = true;
      }
    }
  }
  return pokedResponse;
}

void checkAndSendDRAMResponse() {
  // Check if DRAM is ready
  // TODO: This logic is sketchy for two reasons
  // 1. This checks ready first before asserting valid. This should not be the case; valid and ready must be independent
  // 2. read and write ready signals are combined, so logic stalls if EITHER is not ready. This is quite clunky and doesn't
  //    model real world cases
  SV_BIT_PACKED_ARRAY(32, dramReadRespReady);
  SV_BIT_PACKED_ARRAY(32, dramWriteRespReady);
  getDRAMReadRespReady((svBitVec32*)&dramReadRespReady);
  getDRAMWriteRespReady((svBitVec32*)&dramWriteRespReady);
//  uint32_t readReady = (uint32_t)*dramReadRespReady;
//  uint32_t writeReady = (uint32_t)*dramWriteRespReady;
//  uint32_t ready = readReady & writeReady;
//  if (ready > 0) {

  if (debug) {
    if ((numCycles % 5000) == 0) {
      for (int i = 0; i < MAX_NUM_Q; i++) {
        printQueueStats(i);
      }
    }
  }

  if (popWhenReady >= 0) { // A particular queue has already poked its response, call it again
    ASSERT(checkQAndRespond(popWhenReady), "popWhenReady (%d) >= 0, but no response generated from queue %d\n", popWhenReady, popWhenReady);
  } else {   // Iterate over all queues and respond to the first non-empty queue
    if (useIdealDRAM) {
      for (int i =0; i < MAX_NUM_Q; i++) {
        updateIdealDRAMQ(i);
      }
    }

    for (int i =0; i < MAX_NUM_Q; i++) {
      if (checkQAndRespond(i)) {
        popWhenReady = i;
        break;
      }
    }
  }
//  } else {
//    if (debug) {
//      EPRINTF("[SIM] dramResp not ready, numCycles = %ld\n", numCycles);
//    }
//  }
}

class DRAMCallbackMethods {
public:
  void txComplete(unsigned id, uint64_t addr, uint64_t tag, uint64_t clock_cycle) {
    DRAMTag cmdTag;
    cmdTag.tag = tag;
    if (debug) {
      EPRINTF("[txComplete] addr = %p, tag = %lx, finished = %lu\n", (void*)addr, cmdTag.tag, numCycles);
    }

    // Find transaction, mark it as done, remove entry from map
    struct AddrTag at(addr, cmdTag);
    std::map<struct AddrTag, DRAMRequest*>::iterator it = addrToReqMap.find(at);
    ASSERT(it != addrToReqMap.end(), "address/tag tuple (%lx, %lx) not found in addrToReqMap!", addr, cmdTag.tag);
    DRAMRequest* req = it->second;
    req->completed = true;
    addrToReqMap.erase(at);
  }
};

extern "C" {
  void serviceWRequest() {

    if (wrequestQ.size() > 0 & wdataQ.size() > 0 && wrequestQ.front()->isWr) {
      DRAMRequest *req = wrequestQ.front();
      WData *data = wdataQ.front();
      wrequestQ.pop_front();
      wdataQ.pop_front();
      int write_all = (data->wstrb[0] == 1 && data->wstrb[1] == 1 && data->wstrb[2] == 1 && data->wstrb[3] == 1 && data->wstrb[4] == 1 && data->wstrb[5] == 1 && data->wstrb[6] == 1 && data->wstrb[7] == 1 && data->wstrb[8] == 1 && data->wstrb[9] == 1 && data->wstrb[10] == 1 && data->wstrb[11] == 1 && data->wstrb[12] == 1 && data->wstrb[13] == 1 && data->wstrb[14] == 1 && data->wstrb[15] == 1 && data->wstrb[16] == 1 && data->wstrb[17] == 1 && data->wstrb[18] == 1 && data->wstrb[19] == 1 && data->wstrb[20] == 1 && data->wstrb[21] == 1 && data->wstrb[22] == 1 && data->wstrb[23] == 1 && data->wstrb[24] == 1 && data->wstrb[25] == 1 && data->wstrb[26] == 1 && data->wstrb[27] == 1 && data->wstrb[28] == 1 && data->wstrb[29] == 1 && data->wstrb[30] == 1 && data->wstrb[31] == 1 && data->wstrb[32] == 1 && data->wstrb[33] == 1 && data->wstrb[34] == 1 && data->wstrb[35] == 1 && data->wstrb[36] == 1 && data->wstrb[37] == 1 && data->wstrb[38] == 1 && data->wstrb[39] == 1 && data->wstrb[40] == 1 && data->wstrb[41] == 1 && data->wstrb[42] == 1 && data->wstrb[43] == 1 && data->wstrb[44] == 1 && data->wstrb[45] == 1 && data->wstrb[46] == 1 && data->wstrb[47] == 1 && data->wstrb[48] == 1 && data->wstrb[49] == 1 && data->wstrb[50] == 1 && data->wstrb[51] == 1 && data->wstrb[52] == 1 && data->wstrb[53] == 1 && data->wstrb[54] == 1 && data->wstrb[55] == 1 && data->wstrb[56] == 1 && data->wstrb[57] == 1 && data->wstrb[58] == 1 && data->wstrb[59] == 1 && data->wstrb[60] == 1 && data->wstrb[61] == 1 && data->wstrb[62] == 1 && data->wstrb[63] == 1);
      if (debug) {
        EPRINTF("[Service W Request (wrequestQ: %d elements, wdataQ: %d elements remaining)]\n", wrequestQ.size(), wdataQ.size());
        req->print();
      }

      if (write_all) {
        if (debug) {
          EPRINTF("[Servicing W Command (all channels on)]             %u %u %u %u\n", data->wdata[0], data->wdata[1], data->wdata[2], data->wdata[3]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[4], data->wdata[5], data->wdata[6], data->wdata[7]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[8], data->wdata[9], data->wdata[10], data->wdata[11]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[12], data->wdata[13], data->wdata[14], data->wdata[15]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[16], data->wdata[17], data->wdata[18], data->wdata[19]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[20], data->wdata[21], data->wdata[22], data->wdata[23]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[24], data->wdata[25], data->wdata[26], data->wdata[27]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[28], data->wdata[29], data->wdata[30], data->wdata[31]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[32], data->wdata[33], data->wdata[34], data->wdata[35]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[36], data->wdata[37], data->wdata[38], data->wdata[39]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[40], data->wdata[41], data->wdata[42], data->wdata[43]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[44], data->wdata[45], data->wdata[46], data->wdata[47]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[48], data->wdata[49], data->wdata[50], data->wdata[51]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[52], data->wdata[53], data->wdata[54], data->wdata[55]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[56], data->wdata[57], data->wdata[58], data->wdata[59]);
          EPRINTF("                                                    %u %u %u %u\n", data->wdata[60], data->wdata[61], data->wdata[62], data->wdata[63]);
        }
        req->wdata = data->wdata;
        req->schedule();
      } else {
        // Start with burst data
        uint8_t *wdata = (uint8_t*) malloc(burstSizeBytes);
        uint8_t *raddr = (uint8_t*) req->addr;
        for (int i=0; i<burstSizeWords; i++) {
          wdata[i] = raddr[i];
        }

        if (debug) {
          EPRINTF("[Servicing W Command (Strobed) ]   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[0],  wdata[0],  data->wstrb[0],  data->wdata[1],  wdata[1],  data->wstrb[1],  data->wdata[2],  wdata[2],  data->wstrb[2],   data->wdata[3],  wdata[3],  data->wstrb[3]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[4],  wdata[4],  data->wstrb[4],  data->wdata[5],  wdata[5],  data->wstrb[5],  data->wdata[6],  wdata[6],  data->wstrb[6],   data->wdata[7],  wdata[7],  data->wstrb[7]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[8],  wdata[8],  data->wstrb[8],  data->wdata[9],  wdata[9],  data->wstrb[9],  data->wdata[10], wdata[10], data->wstrb[10],  data->wdata[11], wdata[11], data->wstrb[11]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[12], wdata[12], data->wstrb[12], data->wdata[13], wdata[13], data->wstrb[13], data->wdata[14], wdata[14], data->wstrb[14],  data->wdata[15], wdata[15], data->wstrb[15]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[16], wdata[16], data->wstrb[16], data->wdata[17], wdata[17], data->wstrb[17], data->wdata[18], wdata[18], data->wstrb[18],  data->wdata[19], wdata[19], data->wstrb[19]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[20], wdata[20], data->wstrb[20], data->wdata[21], wdata[21], data->wstrb[21], data->wdata[22], wdata[22], data->wstrb[22],  data->wdata[23], wdata[23], data->wstrb[23]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[24], wdata[24], data->wstrb[24], data->wdata[25], wdata[25], data->wstrb[25], data->wdata[26], wdata[26], data->wstrb[26],  data->wdata[27], wdata[27], data->wstrb[27]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[28], wdata[28], data->wstrb[28], data->wdata[29], wdata[29], data->wstrb[29], data->wdata[30], wdata[30], data->wstrb[30],  data->wdata[31], wdata[31], data->wstrb[31]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[32], wdata[32], data->wstrb[32], data->wdata[33], wdata[33], data->wstrb[33], data->wdata[34], wdata[34], data->wstrb[34],  data->wdata[35], wdata[35], data->wstrb[35]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[36], wdata[36], data->wstrb[36], data->wdata[37], wdata[37], data->wstrb[37], data->wdata[38], wdata[38], data->wstrb[38],  data->wdata[39], wdata[39], data->wstrb[39]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[40], wdata[40], data->wstrb[40], data->wdata[41], wdata[41], data->wstrb[41], data->wdata[42], wdata[42], data->wstrb[42],  data->wdata[43], wdata[43], data->wstrb[43]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[44], wdata[44], data->wstrb[44], data->wdata[45], wdata[45], data->wstrb[45], data->wdata[46], wdata[46], data->wstrb[46],  data->wdata[47], wdata[47], data->wstrb[47]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[48], wdata[48], data->wstrb[48], data->wdata[49], wdata[49], data->wstrb[49], data->wdata[50], wdata[50], data->wstrb[50],  data->wdata[51], wdata[51], data->wstrb[51]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[52], wdata[52], data->wstrb[52], data->wdata[53], wdata[53], data->wstrb[53], data->wdata[54], wdata[54], data->wstrb[54],  data->wdata[55], wdata[55], data->wstrb[55]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[56], wdata[56], data->wstrb[56], data->wdata[57], wdata[57], data->wstrb[57], data->wdata[58], wdata[58], data->wstrb[58],  data->wdata[59], wdata[59], data->wstrb[59]);
          EPRINTF("                                   %u -> %u (%d), %u -> %u (%d), %u -> %u (%d), %u -> %u (%d)\n", data->wdata[60], wdata[60], data->wstrb[60], data->wdata[61], wdata[61], data->wstrb[61], data->wdata[62], wdata[62], data->wstrb[62],  data->wdata[63], wdata[63], data->wstrb[63]);
        }

        // Fill in accel wdata
        if (data->wstrb[0]) wdata[0] = data->wdata[0];
        if (data->wstrb[1]) wdata[1] = data->wdata[1];
        if (data->wstrb[2]) wdata[2] = data->wdata[2];
        if (data->wstrb[3]) wdata[3] = data->wdata[3];
        if (data->wstrb[4]) wdata[4] = data->wdata[4];
        if (data->wstrb[5]) wdata[5] = data->wdata[5];
        if (data->wstrb[6]) wdata[6] = data->wdata[6];
        if (data->wstrb[7]) wdata[7] = data->wdata[7];
        if (data->wstrb[8]) wdata[8] = data->wdata[8];
        if (data->wstrb[9]) wdata[9] = data->wdata[9];
        if (data->wstrb[10]) wdata[10] = data->wdata[10];
        if (data->wstrb[11]) wdata[11] = data->wdata[11];
        if (data->wstrb[12]) wdata[12] = data->wdata[12];
        if (data->wstrb[13]) wdata[13] = data->wdata[13];
        if (data->wstrb[14]) wdata[14] = data->wdata[14];
        if (data->wstrb[15]) wdata[15] = data->wdata[15];
        if (data->wstrb[16]) wdata[16] = data->wdata[16];
        if (data->wstrb[17]) wdata[17] = data->wdata[17];
        if (data->wstrb[18]) wdata[18] = data->wdata[18];
        if (data->wstrb[19]) wdata[19] = data->wdata[19];
        if (data->wstrb[20]) wdata[20] = data->wdata[20];
        if (data->wstrb[21]) wdata[21] = data->wdata[21];
        if (data->wstrb[22]) wdata[22] = data->wdata[22];
        if (data->wstrb[23]) wdata[23] = data->wdata[23];
        if (data->wstrb[24]) wdata[24] = data->wdata[24];
        if (data->wstrb[25]) wdata[25] = data->wdata[25];
        if (data->wstrb[26]) wdata[26] = data->wdata[26];
        if (data->wstrb[27]) wdata[27] = data->wdata[27];
        if (data->wstrb[28]) wdata[28] = data->wdata[28];
        if (data->wstrb[29]) wdata[29] = data->wdata[29];
        if (data->wstrb[30]) wdata[30] = data->wdata[30];
        if (data->wstrb[31]) wdata[31] = data->wdata[31];
        if (data->wstrb[32]) wdata[32] = data->wdata[32];
        if (data->wstrb[33]) wdata[33] = data->wdata[33];
        if (data->wstrb[34]) wdata[34] = data->wdata[34];
        if (data->wstrb[35]) wdata[35] = data->wdata[35];
        if (data->wstrb[36]) wdata[36] = data->wdata[36];
        if (data->wstrb[37]) wdata[37] = data->wdata[37];
        if (data->wstrb[38]) wdata[38] = data->wdata[38];
        if (data->wstrb[39]) wdata[39] = data->wdata[39];
        if (data->wstrb[40]) wdata[40] = data->wdata[40];
        if (data->wstrb[41]) wdata[41] = data->wdata[41];
        if (data->wstrb[42]) wdata[42] = data->wdata[42];
        if (data->wstrb[43]) wdata[43] = data->wdata[43];
        if (data->wstrb[44]) wdata[44] = data->wdata[44];
        if (data->wstrb[45]) wdata[45] = data->wdata[45];
        if (data->wstrb[46]) wdata[46] = data->wdata[46];
        if (data->wstrb[47]) wdata[47] = data->wdata[47];
        if (data->wstrb[48]) wdata[48] = data->wdata[48];
        if (data->wstrb[49]) wdata[49] = data->wdata[49];
        if (data->wstrb[50]) wdata[50] = data->wdata[50];
        if (data->wstrb[51]) wdata[51] = data->wdata[51];
        if (data->wstrb[52]) wdata[52] = data->wdata[52];
        if (data->wstrb[53]) wdata[53] = data->wdata[53];
        if (data->wstrb[54]) wdata[54] = data->wdata[54];
        if (data->wstrb[55]) wdata[55] = data->wdata[55];
        if (data->wstrb[56]) wdata[56] = data->wdata[56];
        if (data->wstrb[57]) wdata[57] = data->wdata[57];
        if (data->wstrb[58]) wdata[58] = data->wdata[58];
        if (data->wstrb[59]) wdata[59] = data->wdata[59];
        if (data->wstrb[60]) wdata[60] = data->wdata[60];
        if (data->wstrb[61]) wdata[61] = data->wdata[61];
        if (data->wstrb[62]) wdata[62] = data->wdata[62];
        if (data->wstrb[63]) wdata[63] = data->wdata[63];


        req->wdata = wdata;
        req->schedule();
      }
    } else if (wdataQ.size() > 0 & wrequestQ.size() == 0) {
      if (debug) {
        EPRINTF("[WARN] WRequestQ empty or head is ~isWr but WDataQ is not empty!");
      }      
    }

  }

  int sendWdataStrb(
    int dramCmdValid,
    int dramReadySeen,
    int wdata0, int wdata1, int wdata2, int wdata3, int wdata4, int wdata5, int wdata6, int wdata7, int wdata8, int wdata9, int wdata10, int wdata11, int wdata12, int wdata13, int wdata14, int wdata15, int wdata16, int wdata17, int wdata18, int wdata19, int wdata20, int wdata21, int wdata22, int wdata23, int wdata24, int wdata25, int wdata26, int wdata27, int wdata28, int wdata29, int wdata30, int wdata31, int wdata32, int wdata33, int wdata34, int wdata35, int wdata36, int wdata37, int wdata38, int wdata39, int wdata40, int wdata41, int wdata42, int wdata43, int wdata44, int wdata45, int wdata46, int wdata47, int wdata48, int wdata49, int wdata50, int wdata51, int wdata52, int wdata53, int wdata54, int wdata55, int wdata56, int wdata57, int wdata58, int wdata59, int wdata60, int wdata61, int wdata62, int wdata63, int strb0,
    int strb1, int strb2, int strb3, int strb4, int strb5, int strb6, int strb7, int strb8, int strb9, int strb10, int strb11, int strb12, int strb13, int strb14, int strb15, int strb16, int strb17, int strb18, int strb19, int strb20, int strb21, int strb22, int strb23, int strb24, int strb25, int strb26, int strb27, int strb28, int strb29, int strb30, int strb31, int strb32, int strb33, int strb34, int strb35, int strb36, int strb37, int strb38, int strb39, int strb40, int strb41, int strb42, int strb43, int strb44, int strb45, int strb46, int strb47, int strb48, int strb49, int strb50, int strb51, int strb52, int strb53, int strb54, int strb55, int strb56, int strb57, int strb58, int strb59, int strb60, int strb61, int strb62, int strb63
  ) {


    WData *data = new WData;
    uint8_t *wdata = (uint8_t*) malloc(burstSizeBytes);
    uint8_t *wstrb = (uint8_t*) malloc(burstSizeBytes);

    // view addr as uint64_t without doing sign extension
    wdata[0] = (*(uint8_t*)&wdata0);
    wdata[1] = (*(uint8_t*)&wdata1);
    wdata[2] = (*(uint8_t*)&wdata2);
    wdata[3] = (*(uint8_t*)&wdata3);
    wdata[4] = (*(uint8_t*)&wdata4);
    wdata[5] = (*(uint8_t*)&wdata5);
    wdata[6] = (*(uint8_t*)&wdata6);
    wdata[7] = (*(uint8_t*)&wdata7);
    wdata[8] = (*(uint8_t*)&wdata8);
    wdata[9] = (*(uint8_t*)&wdata9);
    wdata[10] = (*(uint8_t*)&wdata10);
    wdata[11] = (*(uint8_t*)&wdata11);
    wdata[12] = (*(uint8_t*)&wdata12);
    wdata[13] = (*(uint8_t*)&wdata13);
    wdata[14] = (*(uint8_t*)&wdata14);
    wdata[15] = (*(uint8_t*)&wdata15);
    wdata[16] = (*(uint8_t*)&wdata16);
    wdata[17] = (*(uint8_t*)&wdata17);
    wdata[18] = (*(uint8_t*)&wdata18);
    wdata[19] = (*(uint8_t*)&wdata19);
    wdata[20] = (*(uint8_t*)&wdata20);
    wdata[21] = (*(uint8_t*)&wdata21);
    wdata[22] = (*(uint8_t*)&wdata22);
    wdata[23] = (*(uint8_t*)&wdata23);
    wdata[24] = (*(uint8_t*)&wdata24);
    wdata[25] = (*(uint8_t*)&wdata25);
    wdata[26] = (*(uint8_t*)&wdata26);
    wdata[27] = (*(uint8_t*)&wdata27);
    wdata[28] = (*(uint8_t*)&wdata28);
    wdata[29] = (*(uint8_t*)&wdata29);
    wdata[30] = (*(uint8_t*)&wdata30);
    wdata[31] = (*(uint8_t*)&wdata31);
    wdata[32] = (*(uint8_t*)&wdata32);
    wdata[33] = (*(uint8_t*)&wdata33);
    wdata[34] = (*(uint8_t*)&wdata34);
    wdata[35] = (*(uint8_t*)&wdata35);
    wdata[36] = (*(uint8_t*)&wdata36);
    wdata[37] = (*(uint8_t*)&wdata37);
    wdata[38] = (*(uint8_t*)&wdata38);
    wdata[39] = (*(uint8_t*)&wdata39);
    wdata[40] = (*(uint8_t*)&wdata40);
    wdata[41] = (*(uint8_t*)&wdata41);
    wdata[42] = (*(uint8_t*)&wdata42);
    wdata[43] = (*(uint8_t*)&wdata43);
    wdata[44] = (*(uint8_t*)&wdata44);
    wdata[45] = (*(uint8_t*)&wdata45);
    wdata[46] = (*(uint8_t*)&wdata46);
    wdata[47] = (*(uint8_t*)&wdata47);
    wdata[48] = (*(uint8_t*)&wdata48);
    wdata[49] = (*(uint8_t*)&wdata49);
    wdata[50] = (*(uint8_t*)&wdata50);
    wdata[51] = (*(uint8_t*)&wdata51);
    wdata[52] = (*(uint8_t*)&wdata52);
    wdata[53] = (*(uint8_t*)&wdata53);
    wdata[54] = (*(uint8_t*)&wdata54);
    wdata[55] = (*(uint8_t*)&wdata55);
    wdata[56] = (*(uint8_t*)&wdata56);
    wdata[57] = (*(uint8_t*)&wdata57);
    wdata[58] = (*(uint8_t*)&wdata58);
    wdata[59] = (*(uint8_t*)&wdata59);
    wdata[60] = (*(uint8_t*)&wdata60);
    wdata[61] = (*(uint8_t*)&wdata61);
    wdata[62] = (*(uint8_t*)&wdata62);
    wdata[63] = (*(uint8_t*)&wdata63);

    wstrb[0] =  strb0;
    wstrb[1] =  strb1;
    wstrb[2] =  strb2;
    wstrb[3] =  strb3;
    wstrb[4] =  strb4;
    wstrb[5] =  strb5;
    wstrb[6] =  strb6;
    wstrb[7] =  strb7;
    wstrb[8] =  strb8;
    wstrb[9] =  strb9;
    wstrb[10] = strb10;
    wstrb[11] = strb11;
    wstrb[12] = strb12;
    wstrb[13] = strb13;
    wstrb[14] = strb14;
    wstrb[15] = strb15;
    wstrb[16] = strb16;
    wstrb[17] = strb17;
    wstrb[18] = strb18;
    wstrb[19] = strb19;
    wstrb[20] = strb20;
    wstrb[21] = strb21;
    wstrb[22] = strb22;
    wstrb[23] = strb23;
    wstrb[24] = strb24;
    wstrb[25] = strb25;
    wstrb[26] = strb26;
    wstrb[27] = strb27;
    wstrb[28] = strb28;
    wstrb[29] = strb29;
    wstrb[30] = strb30;
    wstrb[31] = strb31;
    wstrb[32] = strb32;
    wstrb[33] = strb33;
    wstrb[34] = strb34;
    wstrb[35] = strb35;
    wstrb[36] = strb36;
    wstrb[37] = strb37;
    wstrb[38] = strb38;
    wstrb[39] = strb39;
    wstrb[40] = strb40;
    wstrb[41] = strb41;
    wstrb[42] = strb42;
    wstrb[43] = strb43;
    wstrb[44] = strb44;
    wstrb[45] = strb45;
    wstrb[46] = strb46;
    wstrb[47] = strb47;
    wstrb[48] = strb48;
    wstrb[49] = strb49;
    wstrb[50] = strb50;
    wstrb[51] = strb51;
    wstrb[52] = strb52;
    wstrb[53] = strb53;
    wstrb[54] = strb54;
    wstrb[55] = strb55;
    wstrb[56] = strb56;
    wstrb[57] = strb57;
    wstrb[58] = strb58;
    wstrb[59] = strb59;
    wstrb[60] = strb60;
    wstrb[61] = strb61;
    wstrb[62] = strb62;
    wstrb[63] = strb63;

    data->wdata = wdata;
    data->wstrb = wstrb;

    if (debug) {
      EPRINTF("[sendWdataStrb]              %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[0], strb0, wdata[1], strb1, wdata[2], strb2,  wdata[3], strb3);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[4], strb4, wdata[5], strb5, wdata[6], strb6,  wdata[7], strb7);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[8], strb8, wdata[9], strb9, wdata[10], strb10,  wdata[11], strb11);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[12], strb12, wdata[13], strb13, wdata[14], strb14,  wdata[15], strb15);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[16], strb16, wdata[17], strb17, wdata[18], strb18,  wdata[19], strb19);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[20], strb20, wdata[21], strb21, wdata[22], strb22,  wdata[23], strb23);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[24], strb24, wdata[25], strb25, wdata[26], strb26,  wdata[27], strb27);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[28], strb28, wdata[29], strb29, wdata[30], strb30,  wdata[31], strb31);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[32], strb32, wdata[33], strb33, wdata[34], strb34,  wdata[35], strb35);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[36], strb36, wdata[37], strb37, wdata[38], strb38,  wdata[39], strb39);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[40], strb40, wdata[41], strb41, wdata[42], strb42,  wdata[43], strb43);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[44], strb44, wdata[45], strb45, wdata[46], strb46,  wdata[47], strb47);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[48], strb48, wdata[49], strb49, wdata[50], strb50,  wdata[51], strb51);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[52], strb52, wdata[53], strb53, wdata[54], strb54,  wdata[55], strb55);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[56], strb56, wdata[57], strb57, wdata[58], strb58,  wdata[59], strb59);
      EPRINTF("                             %u (%d), %u (%d), %u (%d), %u (%d)\n", wdata[60], strb60, wdata[61], strb61, wdata[62], strb62,  wdata[63], strb63);
    }

    wdataQ.push_back(data);

  }


  int sendDRAMRequest(
      long long addr,
      long long rawAddr,
      int size,
      int tag_uid,
      int tag_streamId,
      int isWr
    ) {
    int dramReady = 1;  // 1 == ready, 0 == not ready (stall upstream)

    // view addr as uint64_t without doing sign extension
    uint64_t cmdAddr = *(uint64_t*)&addr;
    uint64_t cmdRawAddr = *(uint64_t*)&rawAddr;
    DRAMTag cmdTag;
    cmdTag.uid = *(uint32_t*)&tag_uid;
    cmdTag.streamId = *(uint32_t*)&tag_streamId;
    uint32_t cmdSize = (uint32_t)(*(uint32_t*)&size);
    bool cmdIsWr = isWr > 0;

    // Create a DRAM Command
    DRAMCommand *cmd = new DRAMCommand(cmdAddr, cmdSize, cmdTag, cmdIsWr);

    // Create multiple DRAM requests, one per burst
    DRAMRequest **reqs = new DRAMRequest*[cmdSize];
    for (int i = 0; i<cmdSize; i++) {
      reqs[i] = new DRAMRequest(cmdAddr + i*burstSizeBytes, cmdRawAddr + i*burstSizeBytes, cmdSize, cmdTag, cmdIsWr, numCycles);
      reqs[i]->cmd = cmd;
    }
    cmd->reqs = reqs;

    if (debug) {
      EPRINTF("[sendDRAMRequest] Called with ");
      reqs[0]->print();
    }

//    if (!useIdealDRAM) {
      bool skipIssue = false;

      // For each burst request, create an AddrTag
      for (int i=0; i<cmdSize; i++) {
        DRAMRequest *req = reqs[i];
        struct AddrTag at(req->addr, req->tag);

        addrToReqMap[at] = req;
        skipIssue = false;

        // TODO: Re-examine gather-scatter flow
        if (!skipIssue) {
          if (cmdIsWr) {  // Schedule in sendWdata function, when wdata arrives
            wrequestQ.push_back(req);
          } else {
            req->schedule();  // Schedule request with DRAMSim2 / ideal DRAM
          }

        } else {
          if (debug) {
            EPRINTF("                  Skipping addr = %lx (%lx), tag = %lx\n", req->addr, req->rawAddr, req->tag);
          }
        }
      }

    // Push request into appropriate channel queue
    // Note that for ideal DRAM, since "scheduling" a request amounts to pushing the request
    // onto the right dramRequstQ, this happens within the schedule() method in DRAMRequest
    if (dramReady == 1) {
      for (int i=0; i<cmdSize; i++) {
        if (!useIdealDRAM) {
          dramRequestQ[cmdTag.streamId].push_back(reqs[i]);
        }// else {
//          dramRequestQ[reqs[i]->channelID].push_back(reqs[i]);
//        }
      }
    }

    return dramReady;
  }
}

void initDRAM() {
  char *idealDRAM = getenv("USE_IDEAL_DRAM");
  if (idealDRAM != NULL) {
    if (idealDRAM[0] != 0 && atoi(idealDRAM) > 0) {
      useIdealDRAM = true;
    }
  } else {
    useIdealDRAM = false;
  }

  char *debugVar = getenv("DRAM_DEBUG");
  if (debugVar != NULL) {
    if (debugVar[0] != 0 && atoi(debugVar) > 0) {
      debug = true;
      EPRINTF("[DRAM] Verbose debug messages enabled\n");
    }
  } else {
    EPRINTF("[DRAM] Verbose debug messages disabled \n");
    debug = false;
  }

  char *numOutstandingBursts = getenv("DRAM_NUM_OUTSTANDING_BURSTS");
  if (numOutstandingBursts != NULL) {
    if (numOutstandingBursts[0] != 0 && atoi(numOutstandingBursts) > 0) {
      //sparseCacheSize = atoi(numOutstandingBursts);
    }
  }

  char *loadDelay = getenv("N3XT_LOAD_DELAY");
  if (loadDelay != NULL) {
    if (loadDelay[0] != 0 && atoi(loadDelay) > 0) {
      N3XT_LOAD_DELAY = (uint32_t) atoi(loadDelay);
    }
  }
  char *storeDelay = getenv("N3XT_STORE_DELAY");
  if (storeDelay != NULL) {
    if (storeDelay[0] != 0 && atoi(storeDelay) > 0) {
      N3XT_STORE_DELAY = (uint32_t) atoi(storeDelay);
    }
  }
  char *n3xtChannels = getenv("N3XT_NUM_CHANNELS");
  if (n3xtChannels != NULL) {
    if (n3xtChannels[0] != 0 && atoi(n3xtChannels) > 0) {
      N3XT_NUM_CHANNELS = (uint32_t) atoi(n3xtChannels);
    }
  }

  if (useIdealDRAM) {
    ASSERT(N3XT_NUM_CHANNELS < MAX_NUM_Q, "ERROR: N3XT_NUM_CHANNELS (%u) must be lesser than MAX_NUM_Q (%u)\n", N3XT_NUM_CHANNELS, MAX_NUM_Q);
    EPRINTF(" ****** Ideal DRAM configuration ******\n");
    EPRINTF("Num channels         : %u\n", N3XT_NUM_CHANNELS);
    EPRINTF("Load delay (cycles)  : %u\n", N3XT_LOAD_DELAY);
    EPRINTF("Store delay (cycles) : %u\n", N3XT_STORE_DELAY);
    EPRINTF(" **************************************\n");
  }

  if (!useIdealDRAM) {
    // Set up DRAMSim2 - currently hardcoding some values that should later be
    // in a config file somewhere
    char *dramSimHome = getenv("DRAMSIM_HOME");
    ASSERT(dramSimHome != NULL, "ERROR: DRAMSIM_HOME environment variable is not set")
    ASSERT(dramSimHome[0] != NULL, "ERROR: DRAMSIM_HOME environment variable set to null string")


    string memoryIni = string(dramSimHome) + string("/ini/DDR3_micron_32M_8B_x4_sg125.ini");
    string systemIni = string(dramSimHome) + string("spatial.dram.ini");
    // Connect to DRAMSim2 directly here
    mem = DRAMSim::getMemorySystemInstance("ini/DDR3_micron_32M_8B_x4_sg125.ini", "spatial.dram.ini", dramSimHome, "dramSimVCS", 16384);

    uint64_t hardwareClockHz = 1 * 1e9; // Fixing Plasticine clock to 1 GHz
    mem->setCPUClockSpeed(hardwareClockHz);

    // Add callbacks
    DRAMCallbackMethods callbackMethods;
    DRAMSim::TransactionCompleteCB *rwCb = new DRAMSim::Callback<DRAMCallbackMethods, void, unsigned, uint64_t, uint64_t, uint64_t>(&callbackMethods, &DRAMCallbackMethods::txComplete);
    mem->RegisterCallbacks(rwCb, rwCb, NULL);
  }

  // Instantiate 64-to-32-bit address remapper
  remapper = new AddrRemapper();

  // Open trace file
  char *traceFileName = NULL;
  if (useIdealDRAM) {
    traceFileName = "trace_n3xt.log";
  } else {
    traceFileName = "trace_dramsim.log";
  }
  traceFp = fopen(traceFileName, "w");
  ASSERT(traceFp != NULL, "Unable to open file %s!\n", traceFileName);
}

