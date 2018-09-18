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

#include <DRAMSim.h>

#define MAX_NUM_Q             128
#define PAGE_SIZE_BYTES       4096
#define PAGE_OFFSET           (__builtin_ctz(PAGE_SIZE_BYTES))
#define PAGE_FRAME_NUM(addr)  (addr >> PAGE_OFFSET)

// DRAMSim2
DRAMSim::MultiChannelMemorySystem *mem = NULL;
bool useIdealDRAM = false;
bool debug = false;

extern uint64_t numCycles;
uint32_t wordSizeBytes = 4;
uint32_t burstSizeBytes = 64;
uint32_t burstSizeWords = burstSizeBytes / wordSizeBytes;

uint64_t sparseRequestCounter = 0;  // Used to provide unique tags to each sparse request

int sparseCacheSize = -1; // Max. number of lines in cache; -1 == infinity

class DRAMRequest;

/**
 * DRAM Command received from the design
 * One object could potentially create multiple DRAMRequests
 */
class DRAMCommand {
public:
  uint64_t addr;
  uint32_t size;
  uint32_t streamId;
  uint64_t tag;
  uint64_t channelID;
  bool isWr;
  bool isSparse;
  DRAMRequest **reqs = NULL;

  DRAMCommand(uint64_t a, uint32_t sz, uint32_t sid, uint64_t t, bool wr, bool sparse) {
    addr = a;
    size = sz;
    streamId = sid;
    tag = t;
    isWr = wr;
    isSparse = sparse;
    ASSERT(!isSparse | (isSparse & (size == 1)), "ERROR: Sparse requests must have size == 1 (current size: %u)\n", size);
  }

  bool hasCompleted(); // Method definition after 'DRAMRequest' class due to forward reference

  ~DRAMCommand() {
    if (reqs != NULL) free(reqs);
  }
};


/**
 * DRAM Request corresponding to 1-burst that is enqueued into DRAMSim2
 * The 'size' field reflects the size of the entire command of which
 * this request is part of (legacy, should be removed)
 */
class DRAMRequest {
public:
  uint64_t addr;
  uint64_t rawAddr;
  uint32_t size;
  uint32_t streamId;
  uint64_t tag;
  uint64_t sparseTag;
  uint64_t channelID;
  bool isWr;
  bool isSparse;
  uint32_t *wdata = NULL;
  uint32_t delay;
  uint32_t elapsed;
  uint64_t issued;
  bool completed;
  DRAMCommand *cmd;

  DRAMRequest(uint64_t a, uint64_t ra, uint32_t sz, uint32_t sid, uint64_t t, bool wr, bool sparse, uint64_t issueCycle) {
    addr = a;
    rawAddr = ra;
    size = sz;
    streamId = sid;
    tag = t;
    isWr = wr;
    isSparse = sparse;
    sparseTag = -1;

    delay = abs(rand()) % 150 + 50;
    elapsed = 0;
    issued = issueCycle;
    completed = false;
    wdata = NULL;
  }

  void print() {
    EPRINTF("[DRAMRequest CH=%lu] addr: %lx (%lx), sizeInBursts: %u, streamId: %x, tag: %lx, isWr: %d, isSparse: %d, issued=%lu \n", channelID, addr, rawAddr, size, streamId, tag, isWr, isSparse, issued);
  }

  void schedule() {
    uint64_t tagToUse = -1;
    if (isSparse) { // Use sparseTag to schedule sparse req into DRAMSim2, so that we can disambiguate upon txComplete
      tagToUse = sparseTag;
    } else {
      tagToUse = tag;
    }
    mem->addTransaction(isWr, addr, tagToUse);
    channelID = mem->findChannelNumber(addr);
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
  uint64_t tag;

  AddrTag(uint64_t a, uint64_t t) {
    addr = a;
    tag = t;
  }

	bool operator==(const AddrTag &o) const {
			return addr == o.addr && tag == o.tag;
	}

	bool operator<(const AddrTag &o) const {
			return addr < o.addr || (addr == o.addr && tag < o.tag);
	}
};

// DRAM Request Queue
std::deque<DRAMRequest*> dramRequestQ[MAX_NUM_Q];

// WDATA Queue
std::deque<uint32_t*> wdataQ[MAX_NUM_Q];

// Current set of requests that will be getting their wdata in order
std::deque<DRAMRequest*> wrequestQ;

// Internal book-keeping data structures
std::map<struct AddrTag, DRAMRequest*> addrToReqMap;
std::map<struct AddrTag, DRAMRequest**> sparseRequestCache;

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
      EPRINTF("    %d. addr: %lx (%lx), tag: %lx, streamId: %d, sparse = %d, completed: %d\n", k, r->addr, r->rawAddr, r->tag, r->streamId, r->isSparse, r->completed);
      it++;
      k++;
      //    if (k > 20) break;
    }
    EPRINTF("==== END dramRequestQ %d status =====\n", id);

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

  if (req->isSparse) { // Sparse request
    for (int i = 0; i < burstSizeWords; i++) {
      DRAMRequest *head = dramRequestQ[popWhenReady].front();
      ASSERT(head->completed, "ERROR: Encountered sparse request that is not complete when popping sparse requests!\n");
      ASSERT(head->isSparse, "ERROR: Encountered non-sparse request at (%d) while popping sparse requests! (%lx, %lx)\n", i, head->addr, head->rawAddr);
      dramRequestQ[popWhenReady].pop_front();
      delete head;
    }
  } else { // Dense request
    if (req->isWr) { // Write request
      ASSERT(req->cmd->hasCompleted(), "Write command at head of pop queue (%d) is not fully complete!\n", popWhenReady);
      DRAMCommand *cmd = req->cmd;
      DRAMRequest *front = req;
      // Do write data handling, then pop all requests belonging to finished cmd from FIFO
      while ((dramRequestQ[popWhenReady].size() > 0) && (front->cmd == cmd)) {
        uint32_t *front_wdata = front->wdata;
        uint32_t *front_waddr = (uint32_t*) front->addr;
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

    if (useIdealDRAM) {
      req->elapsed++;
      if (req->elapsed >= req->delay) {
        req->completed = true;
        if (debug) {
          EPRINTF("[idealDRAM txComplete] addr = %p, tag = %lx, finished = %lu\n", (void*)req->addr, req->tag, numCycles);
        }
      }
    }

    if (req->completed) {
      if (!req->isSparse) {
        uint32_t rdata[16] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        bool pokeResponse = false;

        if (req->isWr) { // Write request: Update 1 burst-length bytes at *addr
          uint32_t *wdata = req->wdata;
          uint32_t *waddr = (uint32_t*) req->addr;
          for (int i=0; i<burstSizeWords; i++) {
            waddr[i] = wdata[i];
          }
          if (req->cmd->hasCompleted()) {
            pokeResponse = true;
          }
        } else { // Read request: Read burst-length bytes at *addr
          uint32_t *raddr = (uint32_t*) req->addr;
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

          if (req->isWr) {
            pokeDRAMWriteResponse(req->tag);
          } else {
            pokeDRAMReadResponse(
              req->tag,
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
              rdata[15]
            );
          }
          pokedResponse = true;
        }
      } else {
          // Ensure that there are at least 16 requests
          // Ensure that all top 16 requests have been completed
          deque<DRAMRequest*>::iterator it = dramRequestQ[id].begin();
          bool sparseReqCompleted = true;
          if (dramRequestQ[id].size() < burstSizeWords) {
            sparseReqCompleted = false;
          } else {
            for (int i = 0; i < burstSizeWords; i++) {
              DRAMRequest *r = *it;
              if (!r->completed) {
                sparseReqCompleted = false;
                break;
              }
              it++;
            }
          }


          if (sparseReqCompleted) {
            bool writeRequest = req->isWr;
            uint64_t tag = req->tag;

            uint64_t gatherAddr[16] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            uint64_t scatterAddr[16] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            uint64_t scatterData[16] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            uint32_t rdata[16] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            if (debug) {
              EPRINTF("[checkQ] sparse request completed:\n");
              printQueueStats(id);
            }
            it = dramRequestQ[id].begin();
            for (int i = 0; i < burstSizeWords; i++) {
              DRAMRequest *head = *it;
              ASSERT(head->isSparse, "ERROR: Encountered non-sparse request at (%d) while popping sparse requests! (%lx, %lx)", i, head->addr, head->rawAddr);
              ASSERT(head->isWr == writeRequest, "ERROR: Sparse request type mismatch");
              if (!writeRequest) {
                uint32_t *raddr = (uint32_t*) head->rawAddr;
                if (debug) {
                  EPRINTF("-------- gatherAddr(%d) = %lx, boffset = %d. Printing entire burst:\n", i, raddr, head->rawAddr % burstSizeBytes);
                for (int j = 0; j < burstSizeWords; j++) {
                  uint32_t *a = (uint32_t*) (head->addr + j*sizeof(uint32_t));
                  EPRINTF("         [%d] %x (%d)\n", j, *a, *a);
                }

                }
                rdata[i] = *raddr;
                gatherAddr[i] = head->rawAddr;
              } else {
                uint32_t *waddr = (uint32_t*) head->rawAddr;
                uint32_t *wdata = head->wdata; // wdataQ[id].front();
                if (debug) EPRINTF("-------- scatterAddr(%d) = %lx\n", i, waddr);
                *waddr = wdata[0];
                scatterAddr[i] = head->rawAddr;
                scatterData[i] = wdata[0];
              }
              it++;
            }

            if (debug) {
              EPRINTF("[checkAndSendDRAMResponse] Sparse complete with following details:\n");
              for (int i = 0; i<burstSizeWords; i++) {
                if (writeRequest) {
                  EPRINTF("     [scatter] addr %lx: data %x\n", scatterAddr[i], scatterData[i]);
                } else {
                  EPRINTF("     addr %lx: %x (%d)\n", gatherAddr[i], rdata[i], rdata[i]);
                }
              }
              printQueueStats(id);
            }

            if (req->isWr) {
              pokeDRAMWriteResponse(req->tag);
            } else {
              pokeDRAMReadResponse(
                  req->tag,
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
                  rdata[15]
                );
            }
            pokedResponse = true;
          }
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
    if (debug) {
      EPRINTF("[txComplete] addr = %p, tag = %lx, finished = %lu\n", (void*)addr, tag, numCycles);
    }

    // Find transaction, mark it as done, remove entry from map
    struct AddrTag at(addr, tag);
    std::map<struct AddrTag, DRAMRequest*>::iterator it = addrToReqMap.find(at);
    ASSERT(it != addrToReqMap.end(), "address/tag tuple (%lx, %lx) not found in addrToReqMap!", addr, tag);
    DRAMRequest* req = it->second;
    req->completed = true;
    addrToReqMap.erase(at);

    if (req->isSparse) { // Mark all waiting transactions done
      at.tag = at.tag & 0xFFFFFFFF;
      std::map<struct AddrTag, DRAMRequest**>::iterator it = sparseRequestCache.find(at);
      ASSERT(it != sparseRequestCache.end(), "Could not find (%lx, %lx) in sparseRequestCache!", addr, tag);
      DRAMRequest **line = it->second;
      for (int i = 0; i < 16; i++) {
        if (line[i] != NULL) {
          line[i]->completed = true;
        }
      }
      sparseRequestCache.erase(at);
    }

  }
};

bool sparseCacheFull() {
  if (sparseCacheSize == -1) return false;  // sparseRequestCache is infinitely large
  else if (sparseRequestCache.size() <= sparseCacheSize) return false;
  else return true;
}

extern "C" {
  int sendWdata(
      int streamId,
      int dramCmdValid,
      int dramReadySeen,
      int wdata0,
      int wdata1,
      int wdata2,
      int wdata3,
      int wdata4,
      int wdata5,
      int wdata6,
      int wdata7,
      int wdata8,
      int wdata9,
      int wdata10,
      int wdata11,
      int wdata12,
      int wdata13,
      int wdata14,
      int wdata15
    ) {
//    int wdataReady = 1;  // 1 == ready, 0 == not ready (stall upstream)

    // view addr as uint64_t without doing sign extension
    uint32_t cmdWdata0 = (*(uint32_t*)&wdata0);
    uint32_t cmdWdata1 = (*(uint32_t*)&wdata1);
    uint32_t cmdWdata2 = (*(uint32_t*)&wdata2);
    uint32_t cmdWdata3 = (*(uint32_t*)&wdata3);
    uint32_t cmdWdata4 = (*(uint32_t*)&wdata4);
    uint32_t cmdWdata5 = (*(uint32_t*)&wdata5);
    uint32_t cmdWdata6 = (*(uint32_t*)&wdata6);
    uint32_t cmdWdata7 = (*(uint32_t*)&wdata7);
    uint32_t cmdWdata8 = (*(uint32_t*)&wdata8);
    uint32_t cmdWdata9 = (*(uint32_t*)&wdata9);
    uint32_t cmdWdata10 = (*(uint32_t*)&wdata10);
    uint32_t cmdWdata11 = (*(uint32_t*)&wdata11);
    uint32_t cmdWdata12 = (*(uint32_t*)&wdata12);
    uint32_t cmdWdata13 = (*(uint32_t*)&wdata13);
    uint32_t cmdWdata14 = (*(uint32_t*)&wdata14);
    uint32_t cmdWdata15 = (*(uint32_t*)&wdata15);

    uint32_t *wdata = (uint32_t*) malloc(burstSizeBytes);
    wdata[0] = cmdWdata0;
    wdata[1] = cmdWdata1;
    wdata[2] = cmdWdata2;
    wdata[3] = cmdWdata3;
    wdata[4] = cmdWdata4;
    wdata[5] = cmdWdata5;
    wdata[6] = cmdWdata6;
    wdata[7] = cmdWdata7;
    wdata[8] = cmdWdata8;
    wdata[9] = cmdWdata9;
    wdata[10] = cmdWdata10;
    wdata[11] = cmdWdata11;
    wdata[12] = cmdWdata12;
    wdata[13] = cmdWdata13;
    wdata[14] = cmdWdata14;
    wdata[15] = cmdWdata15;

    if (debug) {
      EPRINTF("[sendWdata dramCmdValid: %d, dramReadySeen: %d] %u %u %u %u\n", dramCmdValid, dramReadySeen, cmdWdata0, cmdWdata1, cmdWdata2, cmdWdata3);
      EPRINTF("                                                %u %u %u %u\n", cmdWdata4, cmdWdata5, cmdWdata6, cmdWdata7);
      EPRINTF("                                                %u %u %u %u\n", cmdWdata8, cmdWdata9, cmdWdata10, cmdWdata11);
      EPRINTF("                                                %u %u %u %u\n", cmdWdata12, cmdWdata13, cmdWdata14, cmdWdata15);
    }

    ASSERT(wrequestQ.size() > 0, "Wdata sent when no write requests were seen before!\n");

    DRAMRequest *req = wrequestQ.front();
    wrequestQ.pop_front();
    req->wdata = wdata;
    req->schedule();

//    if (wdataReady == 1) {
//      wdataQ[streamId].push_back(wdata);
//    }
//    return wdataReady;
  }

  int sendDRAMRequest(
      long long addr,
      long long rawAddr,
      int size,
      int streamId,
      int tag,
      int isWr,
      int isSparse
    ) {
    int dramReady = 1;  // 1 == ready, 0 == not ready (stall upstream)

    // view addr as uint64_t without doing sign extension
    uint64_t cmdAddr = *(uint64_t*)&addr;
    uint64_t cmdRawAddr = *(uint64_t*)&rawAddr;
    uint64_t cmdTag = (uint64_t)(*(uint32_t*)&tag);
    uint32_t cmdSize = (uint32_t)(*(uint32_t*)&size);
    uint64_t cmdStreamId = (uint32_t)(*(uint32_t*)&streamId);
    bool cmdIsWr = isWr > 0;
    bool cmdIsSparse = isSparse > 0;

    // Create a DRAM Command
    DRAMCommand *cmd = new DRAMCommand(cmdAddr, cmdSize, cmdStreamId, cmdTag, cmdIsWr, cmdIsSparse);

    // Create multiple DRAM requests, one per burst
    DRAMRequest **reqs = new DRAMRequest*[cmdSize];
    for (int i = 0; i<cmdSize; i++) {
      reqs[i] = new DRAMRequest(cmdAddr + i*burstSizeBytes, cmdRawAddr + i*burstSizeBytes, cmdSize, cmdStreamId, cmdTag, cmdIsWr, cmdIsSparse, numCycles);
      reqs[i]->cmd = cmd;
    }
    cmd->reqs = reqs;

    if (debug) {
      EPRINTF("[sendDRAMRequest] Called with ");
      reqs[0]->print();
    }

    if (!useIdealDRAM) {
      bool skipIssue = false;

      // For each burst request, create an AddrTag
      for (int i=0; i<cmdSize; i++) {
        DRAMRequest *req = reqs[i];
        struct AddrTag at(req->addr, req->tag);

        if (!cmdIsSparse) { // Dense request
          addrToReqMap[at] = req;
          skipIssue = false;
        } else {  // Sparse request
          // Early out if cache is full
          if (sparseCacheFull()) {
            skipIssue = true;
            dramReady = 0;
          } else {
            std::map<struct AddrTag, DRAMRequest**>::iterator it = sparseRequestCache.find(at);
            if (debug) EPRINTF("                  Sparse request, looking up (addr = %lx, tag = %lx)\n", at.addr, at.tag);
            if (it == sparseRequestCache.end()) { // MISS
              if (debug) EPRINTF("                  MISS, creating new cache line:\n");
              skipIssue = false;
              DRAMRequest **line = new DRAMRequest*[16]; // One outstanding request per word
              memset(line, 0, 16*sizeof(DRAMRequest*));
              line[getWordOffset(cmdRawAddr)] = req;
              if (debug) {
                for (int i=0; i<16; i++) {
                  EPRINTF("---- %p ", line[i]);
                }
                EPRINTF("\n");
              }

              sparseRequestCache[at] = line;

              // Disambiguate each request with unique tag in the addr -> req mapping
              uint64_t sparseTag = ((sparseRequestCounter++) << 32) | (cmdTag & 0xFFFFFFFF);
              at.tag = sparseTag;
              req->sparseTag = sparseTag;
              addrToReqMap[at] = req;
            } else {  // HIT
              if (debug) EPRINTF("                  HIT, line:\n");
              skipIssue = true;
              DRAMRequest **line = it->second;
              if (debug) {
                for (int i=0; i<16; i++) {
                  EPRINTF("---- %p ", line[i]);
                }
                EPRINTF("\n");
              }

              DRAMRequest *r = line[getWordOffset(cmdRawAddr)];

              if (r != NULL) {  // Already a request waiting, stall upstream
                if (debug) EPRINTF("                  Req %lx (%lx) already present for given word, stall upstream\n", r->addr, r->rawAddr);
                dramReady = 0;
              } else {  // Update word offset with request pointer
                line[getWordOffset(cmdRawAddr)] = req;
              }
            }
          }
        }

        // TODO: Re-examine gather-scatter flow
        if (!skipIssue) {
          if (cmdIsWr) {  // Schedule in sendWdata function, when wdata arrives
            wrequestQ.push_back(req);
          } else {
            req->schedule();  // Schedule request with DRAMSim2
          }

        } else {
          if (debug) {
            EPRINTF("                  Skipping addr = %lx (%lx), tag = %lx\n", req->addr, req->rawAddr, req->tag);
          }
        }
      }
    }

    if (dramReady == 1) {
      for (int i=0; i<cmdSize; i++) {
        dramRequestQ[cmdStreamId].push_back(reqs[i]);
      }
    }

    return dramReady;
  }
}

void initDRAM() {
  char *idealDRAM = getenv("USE_IDEAL_DRAM");
  EPRINTF("idealDRAM = %s\n", idealDRAM);
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
      sparseCacheSize = atoi(numOutstandingBursts);
    }
  }
  EPRINTF("[DRAM] Sparse cache size = %d\n", sparseCacheSize);


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
}

