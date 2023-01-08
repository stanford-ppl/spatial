#ifndef __FRINGE_CONTEXT_VCS_H__
#define __FRINGE_CONTEXT_VCS_H__

#include <spawn.h>
#include <poll.h>
#include <sys/wait.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>

#include "FringeContextBase.h"
#include "simDefs.h"
#include "channel.h"
#include "generated_debugRegs.h"

using namespace std;

//Source: http://stackoverflow.com/questions/13893085/posix-spawnp-and-piping-child-output-to-a-string
class FringeContextVCS : public FringeContextBase<void> {

  typedef union {
    struct {
        uint64_t done : 1, timeout : 1, allocDealloc : 3, sizeAddr : 59;
    };
    uint64_t bits;
  } status_t;

  pid_t sim_pid;
  Channel *cmdChannel;
  Channel *respChannel;
  int initialCycles = -1;
  uint64_t numCycles = 0;
  uint32_t numArgIns = 0;
  uint32_t numArgInsId = 0;
  uint32_t numArgOuts = 0;
  uint32_t numArgIOs = 0;
  uint32_t numArgIOsId = 0;
  uint32_t numArgOutInstrs = 0;
  uint32_t numArgEarlyExits = 0;

  posix_spawn_file_actions_t action;
  int globalID = 1;

  const uint32_t burstSizeBytes = 64;
  const uint32_t commandReg = 0;
  const uint32_t statusReg = 1;
  uint64_t maxCycles = MAX_CYCLES;
  uint64_t stepCount = 0;

  // Debug flags
  bool debugRegs = false;

  // Set of environment variables that should be set and visible to the simulator process
  // Each variable must be explicitly mentioned here
  // Each specified variable must be set (will trigger an assert otherwise)
  std::vector<std::string> envVariablesToSim = {
    "LD_LIBRARY_PATH",
    "DRAMSIM_HOME",
    "USE_IDEAL_DRAM",
    "DRAM_DEBUG",
    "DRAM_NUM_OUTSTANDING_BURSTS",
    "VPD_ON",
    "VCD_ON",
    "N3XT_LOAD_DELAY",
    "N3XT_STORE_DELAY",
    "N3XT_NUM_CHANNELS"
  };

  char* checkAndGetEnvVar(std::string var) {
    const char *cvar = var.c_str();
    char *value = getenv(cvar);
    ASSERT(value != NULL, "%s is NULL\n", cvar);
    return value;
  }

  bool envToBool(std::string var) {
    const char *cvar = var.c_str();
    char *value = getenv(cvar);
    if (value != NULL) {
      if (value[0] != 0 && atoi(value) > 0) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  long envToLong(std::string var) {
    const char *cvar = var.c_str();
    char *value = getenv(cvar);
    if (value != NULL) {
      if (value[0] != 0) {
        return atol(value);
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  }

  int sendCmd(SIM_CMD cmd) {
    simCmd simCmd;
    simCmd.id = globalID++;
    simCmd.cmd = cmd;

    switch (cmd) {
      case RESET:
        simCmd.size = 0;
        break;
      case START:
        simCmd.size = 0;
        break;
      case STEP:
        simCmd.size = 0;
        break;
      case FIN:
        simCmd.size = 0;
        break;
      case READY:
        simCmd.size = 0;
        break;
      case GET_CYCLES:
        simCmd.size = 0;
        break;
      default:
        EPRINTF("Command %d not supported!\n", cmd);
        exit(-1);
    }

    cmdChannel->send(&simCmd);
    return simCmd.id;
  }

  simCmd* recvResp() {
    return (simCmd*) respChannel->recv();
  }

public:
  void step() {
    sendCmd(STEP);
    numCycles = getCycles();
    stepCount++;

    int printInterval = 10000;
    static int nextPrint = printInterval;

    if ((numCycles >= nextPrint)) {
      EPRINTF("\t%lu cycles elapsed\n", (long unsigned int) nextPrint);
      nextPrint += printInterval;
    }
  }

  void flushCache(uint32_t kb) {
  }

  uint64_t getCycles() {
    int id = sendCmd(GET_CYCLES);
    simCmd *resp = recvResp();
    if (id != resp->id) {
        std::cout << "Malloc Resp->ID = " << resp->id << " does not match cmd.id " << id << std::endl;
    }
    ASSERT(id == resp->id, "GET_CYCLES resp->id does not match cmd.id!");
    ASSERT(GET_CYCLES == resp->cmd, "GET_CYCLES resp->cmd does not match cmd.cmd!");
    uint64_t cycles = *(uint64_t*)resp->data;
    if (initialCycles == -1) initialCycles = (int) cycles;
    return (cycles - initialCycles);
  }

  void finish() {
    int id = sendCmd(FIN);
    simCmd *resp = recvResp();
    ASSERT(id == resp->id, "FIN resp->id does not match cmd.id!");
    ASSERT(FIN == resp->cmd, "FIN resp->cmd does not match cmd.cmd!");
  }

  void reset() {
    sendCmd(RESET);
  }

  void start() {
    sendCmd(START);
  }

  virtual void writeReg(uint32_t reg, uint64_t data) {
    simCmd cmd;
    cmd.id = globalID++;
    cmd.cmd = WRITE_REG;
    std::memcpy(cmd.data, &reg, sizeof(uint32_t));
    std::memcpy(cmd.data+sizeof(uint32_t), &data, sizeof(uint64_t));
    cmd.size = sizeof(uint64_t);
    cmdChannel->send(&cmd);
  }

  virtual uint64_t readReg(uint32_t reg) {
    simCmd cmd;
    simCmd *resp = NULL;
    cmd.id = globalID++;
    cmd.cmd = READ_REG;
    cmd.size = 0;
    std::memcpy(cmd.data, &reg, sizeof(uint32_t));
    cmdChannel->send(&cmd);
    resp = recvResp();
    ASSERT(resp->cmd == READ_REG, "Response from Sim is not READ_REG");
    uint64_t rdata = *(uint64_t*)resp->data;
    return rdata;
  }

  virtual uint64_t malloc(size_t bytes) {
    size_t safe_bytes = std::max(sizeof(size_t),bytes); // Hack in case malloc is size 0
    simCmd cmd;
    cmd.id = globalID++;
    cmd.cmd = MALLOC;
    std::memcpy(cmd.data, &safe_bytes, sizeof(size_t));
    cmd.size = sizeof(size_t);
    cmdChannel->send(&cmd);
    simCmd *resp = recvResp();
    if (cmd.id != resp->id) {
        std::cout << "Malloc Resp->ID = " << resp->id << " does not match cmd.id " << cmd.id << std::endl;
    }
    ASSERT(cmd.id == resp->id, "malloc resp->id does not match cmd.id!");
    ASSERT(cmd.cmd == resp->cmd, "malloc resp->cmd does not match cmd.cmd!");
    return (uint64_t)(*(uint64_t*)resp->data);
  }

  virtual void free(uint64_t buf) {
    simCmd cmd;
    cmd.id = globalID++;
    cmd.cmd = FREE;
    std::memcpy(cmd.data, &buf, sizeof(uint64_t));
    cmd.size = sizeof(uint64_t);
    cmdChannel->send(&cmd);
  }

  virtual void memcpy(uint64_t dst, void *src, size_t bytes) {
    simCmd cmd;
    cmd.id = globalID++;
    cmd.cmd = MEMCPY_H2D;
    uint64_t *data = (uint64_t*)cmd.data;
    data[0] = dst;
    data[1] = bytes;
    cmd.size = 2* sizeof(uint64_t);

    if (src) {
      cmdChannel->send(&cmd);
  
      // Now send fixed 'bytes' from src
      cmdChannel->sendFixedBytes(src, bytes);

      // Wait for ack
      simCmd *resp = recvResp();
      ASSERT(cmd.id == resp->id, "memcpy resp->id does not match cmd.id!");
      ASSERT(cmd.cmd == resp->cmd, "memcpy resp->cmd does not match cmd.cmd!");      
    }
  }

  virtual void memcpy(void *dst, uint64_t src, size_t bytes) {
    simCmd cmd;
    cmd.id = globalID++;
    cmd.cmd = MEMCPY_D2H;
    uint64_t *data = (uint64_t*)cmd.data;
    data[0] = src;
    data[1] = bytes;
    cmd.size = 2* sizeof(uint64_t);

    if (dst) {
      cmdChannel->send(&cmd);

      // Now receive fixed 'bytes' from src
      respChannel->recvFixedBytes(dst, bytes);      
    }
  }

  void connect() {
    int id = sendCmd(READY);
    simCmd *cmd = recvResp();
    EPRINTF("Sent cmd id %d payload %d. Echoed cmd id %d payload %d\n", id, READY, cmd->id, cmd->cmd);
    ASSERT(cmd->id == id, "Error: Received ID does not match sent ID\n");
    ASSERT(cmd->cmd == READY, "Error: Received cmd is not 'READY'\n");
    EPRINTF("Connection successful!\n");
  }

  FringeContextVCS(std::string path = "") : FringeContextBase(path) {
    cmdChannel = new Channel(sizeof(simCmd));
    respChannel = new Channel(sizeof(simCmd));

    posix_spawn_file_actions_init(&action);

    // Create cmdPipe (read) handle at SIM_CMD_FD, respPipe (write) handle at SIM_RESP_FD
    // Close old descriptors after dup2
    posix_spawn_file_actions_addclose(&action, cmdChannel->writeFd());
    posix_spawn_file_actions_addclose(&action, respChannel->readFd());
    posix_spawn_file_actions_adddup2(&action, cmdChannel->readFd(), SIM_CMD_FD);
    posix_spawn_file_actions_adddup2(&action, respChannel->writeFd(), SIM_RESP_FD);

    std::string argsmem[] = {path};
    char *args[] = {&argsmem[0][0],nullptr};

    // Pass required environment variables to simulator
    // Required environment variables must be specified in "envVariablesToSim"
    char **envs = new char*[envVariablesToSim.size() + 1];
    std::string *valueStrs = new std::string[envVariablesToSim.size()];
    int i = 0;
    for (std::vector<std::string>::iterator it = envVariablesToSim.begin(); it != envVariablesToSim.end(); it++) {
      std::string var = *it;
      valueStrs[i] = var + "=" + string(checkAndGetEnvVar(var));
      envs[i] = &valueStrs[i][0];
      i++;
    }
    envs[envVariablesToSim.size()] = nullptr;

    if(posix_spawnp(&sim_pid, args[0], &action, NULL, &args[0], &envs[0]) != 0) {
      EPRINTF("posix_spawnp failed, error = %s\n", strerror(errno));
      exit(-1);
    }

    // Close Sim side of pipes
    close(cmdChannel->readFd());
    close(respChannel->writeFd());

    // Connect with simulator
    EPRINTF("FPGA PID is %d\n", sim_pid);
    connect();

    // Configure settings from environment
    debugRegs = envToBool("DEBUG_REGS");
    long envCycles = atol("MAX_CYCLES");
    if (envCycles > 0) maxCycles = envCycles;

    EPRINTF("DEBUG Regs: %d\n", debugRegs);
    EPRINTF("Env Cycles: %d\n", envCycles);
  }

  virtual void load() {
    for (int i=0; i<5; i++) {
      reset();
    }
    start();
  }

  virtual void run() {
    // Current assumption is that the design sets arguments individually
    status_t status = { .bits = 0 };

    // Implement 4-way handshake
    writeReg(statusReg, 0);
    writeReg(commandReg, 2);
    sleep(0.1);
    writeReg(commandReg, 0);
    sleep(0.1);
    writeReg(commandReg, 1);

    while((!status.done && !status.timeout) && (numCycles <= maxCycles)) {
      step();
      status.bits = readReg(statusReg);
      switch (status.allocDealloc) {
        case 1:
          status.allocDealloc = 3;
          status.sizeAddr = (uint64_t)malloc(status.sizeAddr);
          writeReg(statusReg, status.bits);
          break;
        case 2:
          status.allocDealloc = 4;
          free(status.sizeAddr);
          writeReg(statusReg, status.bits);
          break;
        default:
          break;
      }
    }
    EPRINTF("Design ran for %lu cycles, status = %lu\n", numCycles, status.bits);
    if (!status.done && !status.timeout) { // Design did not run to completion
      EPRINTF("=========================================\n");
      EPRINTF("ERROR: Simulation terminated after %lu cycles\n", numCycles);
      EPRINTF("=========================================\n");
      dumpAllRegs();
    } else {  // Ran to completion, pull down command signal
      if (status.timeout) { // Hardware timeout
        EPRINTF("=========================================\n");
        EPRINTF("Hardware timeout after %lu cycles\n", numCycles);
        EPRINTF("=========================================\n");
      }
     sleep(1);
     dumpAllRegs();
     // dumpDebugRegs();
      writeReg(commandReg, 0);
      while (status.done || status.timeout) {
        step();
        status.bits = readReg(statusReg);
      }
    }
  }

  virtual void setNumArgIns(uint32_t number) {
    numArgIns = number;
  }

  virtual void setNumEarlyExits(uint32_t number) {
    numArgEarlyExits = number;
  }

  virtual void setNumArgIOs(uint32_t number) {
    numArgIOs = number;
  }

  virtual void setNumArgOuts(uint32_t number) {
    numArgOuts = number;
  }

  virtual void setNumArgOutInstrs(uint32_t number) {
    numArgOutInstrs = number;
  }
  
  virtual void setArg(uint32_t arg, uint64_t data, bool isIO) {
    writeReg(arg+2, data);
    numArgInsId++;
    if (isIO) numArgIOsId++;
  }

  virtual uint64_t getArg64(uint32_t arg, bool isIO) {
    return getArg(arg, isIO);
  }

  virtual uint64_t getArg(uint32_t arg, bool isIO) {
    if (numArgIns == 0) return readReg(3+arg);
    else return readReg(2+arg);
    // if (isIO) {
    //   return readReg(2+arg);
    // } else {
    //   if (numArgIns == 0) {
    //     return readReg(1-numArgIOs+2+arg);
    //   } else {
    //     return readReg(numArgIns-numArgIOs+2+arg);
    //   }

    // }
  }

  virtual uint64_t getArgIn(uint32_t arg, bool isIO) {
    return readReg(2+arg);
  }

  void dumpAllRegs() {
    int argIns = numArgIns == 0 ? 1 : numArgIns;
    int argOuts = (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0) ? 1 : numArgOuts;
    int debugRegStart = 2 + argIns + argOuts + numArgOutInstrs + numArgEarlyExits;
    int totalRegs = argIns + argOuts + numArgOutInstrs + numArgEarlyExits + 2 + NUM_DEBUG_SIGNALS;
    EPRINTF("Dumping %d Regs:", totalRegs);
    for (int i=0; i<totalRegs; i++) {
      uint32_t value = readReg(i);
      if (i < debugRegStart) {
        if (i == 0) EPRINTF(" ******* Non-debug regs *******\n");
        EPRINTF("\tR%d: %08x (%08d)\n", i, value, value);
      } else {
        if (i == debugRegStart) EPRINTF("\n\n ******* Debug regs *******\n");
        EPRINTF("\tR%d %s: %08x (%08d)\n", i, signalLabels[i - debugRegStart], value, value);
      }
    }
  }

  void dumpNonDebugRegs() {
    int argIns = numArgIns == 0 ? 1 : numArgIns;
    int argOuts = (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0) ? 1 : numArgOuts;
    int debugRegStart = 2 + argIns + argOuts + numArgOutInstrs + numArgEarlyExits;

    for (int i=0; i<debugRegStart; i++) {
      uint64_t value = readReg(i);
      if (i < debugRegStart) {
        if (i == 0) EPRINTF(" ******* Non-debug regs *******\n");
        EPRINTF("\tR%d: %016lx (%08lu)\n", i, value, value);
      }
    }
  }

  void dumpDebugRegs() {
    EPRINTF(" ******* Debug regs *******\n");
    int argInOffset = numArgIns == 0 ? 1 : numArgIns;
    int argOutOffset = (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0) ? 1 : numArgOuts;
    for (int i=0; i<NUM_DEBUG_SIGNALS; i++) {
      if (i % 16 == 0) EPRINTF("\n");
      uint64_t value = readReg(argInOffset + argOutOffset + numArgOutInstrs + numArgEarlyExits + 2 - numArgIOs + i);
      EPRINTF("\t%s: %08lx (%08lx)\n", signalLabels[i], value, value);
    }
    EPRINTF(" **************************\n");
  }

  ~FringeContextVCS() {
    if (debugRegs) {
      dumpAllRegs();
    }
    finish();
  }
};

// Fringe Simulation APIs
void fringeInit(int argc, char **argv) {
}

#endif
