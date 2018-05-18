#ifndef __FRINGE_CONTEXT_ZCU_H__
#define __FRINGE_CONTEXT_ZCU_H__

#include "FringeContextBase.h"
#include "ZCUAddressMap.h"
#include "ZCUUtils.h"
#include <cstring>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <errno.h>
#include <unistd.h>
#include <time.h>
#include "generated_debugRegs.h"
// #include <xil_cache.h>
// #include <xil_io.h>

// Some key code snippets have been borrowed from the following source:
// https://shanetully.com/2014/12/translating-virtual-addresses-to-physcial-addresses-in-user-space

// The page frame shifted left by PAGE_SHIFT will give us the physcial address of the frame
// // Note that this number is architecture dependent. For me on x86_64 with 4096 page sizes,
// // it is defined as 12. If you're running something different, check the kernel source
// // for what it is defined as.
#define PAGE_SHIFT 12
#define PAGEMAP_LENGTH 8
#define USE_PHYS_ADDR

/**
 * ZCU Fringe Context
 */

// extern "C" {
//   void __clear_cache(char* beg, char* end);
//   void Xil_DCacheFlushRange(INTPTR adr, INTPTR len);

// }

class FringeContextZCU : public FringeContextBase<void> {

  uint32_t burstSizeBytes;
  int fd;
  u64 fringeScalarBase;
  u64 fringeMemBase;
  u64 fpgaMallocPtr;
  u64 fpgaFreeMemSize;
  u64 resetHandshakePtr;

  u64 commandReg;
  u64 statusReg;

  std::map<uint64_t, void*> physToVirtMap;

  void* physToVirt(uint64_t physAddr) {
    std::map<uint64_t, void*>::iterator iter = physToVirtMap.find(physAddr);
    if (iter == physToVirtMap.end()) {
      EPRINTF("Physical address '%lx' not found in physToVirtMap\n. Was this allocated before?", physAddr);
      exit(-1);
    }
    return iter->second;
  }

  uint64_t virtToPhys(void *virt) {
    uint64_t phys = 0;

    // Open the pagemap file for the current process
    FILE *pagemap = fopen("/proc/self/pagemap", "rb");
    FILE *origmap = pagemap;

    // Seek to the page that the buffer is on
    unsigned long offset = (unsigned long)virt/ getpagesize() * PAGEMAP_LENGTH;
    if(fseek(pagemap, (unsigned long)offset, SEEK_SET) != 0) {
      fprintf(stderr, "Failed to seek pagemap to proper location\n");
      exit(1);
    }

    // The page frame number is in bits 0-54 so read the first 7 bytes and clear the 55th bit
    unsigned long page_frame_number = 0;
    fread(&page_frame_number, 1, PAGEMAP_LENGTH-1, pagemap);

    page_frame_number &= 0x7FFFFFFFFFFFFF;

    fclose(origmap);

    // Find the difference from the virt to the page boundary
    unsigned int distance_from_page_boundary = (unsigned long)virt % getpagesize();
    // Determine how far to seek into memory to find the virt
    phys = (page_frame_number << PAGE_SHIFT) + distance_from_page_boundary;

    return phys;
  }

public:
  uint32_t numArgIns;
  uint32_t numArgIOs;
  uint32_t numArgOuts;
  uint32_t numArgOutInstrs;
  uint32_t numArgEarlyExits;
  std::string bitfile;

  FringeContextZCU(std::string path = "") : FringeContextBase(path) {
    bitfile = path;

    numArgIns = 0;
    numArgIOs = 0;
    numArgOuts = 0;
    numArgOutInstrs = 0;
    numArgEarlyExits = 0;
    commandReg = 0;
    statusReg = 1;
    fd = 0;
    burstSizeBytes = 64;
    fringeScalarBase = 0;
    fringeMemBase = 0;
    fpgaMallocPtr = 0;
    fpgaFreeMemSize = MEM_SIZE;
    // open /dev/mem file
    int retval = setuid(0);
    ASSERT(retval == 0, "setuid(0) failed\n");
    fd = open ("/dev/mem", O_RDWR);
    if (fd < 1) {
      perror("error opening /dev/mem\n");
    }

    // Initialize pointers to fringeScalarBase
    void *ptr;
    ptr = mmap(NULL, MAP_LEN, PROT_READ|PROT_WRITE, MAP_SHARED, fd, FRINGE_SCALAR_BASEADDR);
    fringeScalarBase = (u64) ptr;
    EPRINTF("placing fringeScalarBase at %lx\n", fringeScalarBase);

    // Initialize pointer to fringeMemBase
    ptr = mmap(NULL, MEM_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, fd, FRINGE_MEM_BASEADDR);
    fringeMemBase = (u64) ptr;
    EPRINTF("placing fringeMemBase at %lx\n", fringeMemBase);
    fpgaMallocPtr = fringeMemBase;

    // Initialize pointer to Xilinx reset handshake
    ptr = mmap(NULL, RESET_HANDSHAKE_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, fd, RESET_HANDSHAKE_START);
    resetHandshakePtr = (u64) ptr;
    EPRINTF("placing resetHandshakePtr at %lx\n", resetHandshakePtr);

  }

  void PSU_Mask_Write(u64 offset, u32 mask, u32 val) {
    u64 virtOfs = resetHandshakePtr + (offset - RESET_HANDSHAKE_START);
    // EPRINTF("Computing: %lx = %lx + %lx - %lx\n", virtOfs, resetHandshakePtr, offset, RESET_HANDSHAKE_START);
    u32 RegVal = 0x0;

    RegVal = Xil_In32(virtOfs);
    EPRINTF("before: addr %lx has %lx\n", offset, RegVal);
    RegVal &= ~(mask);
    RegVal |= (val & mask);
    Xil_Out32(virtOfs, RegVal);
    EPRINTF("after: addr %lx has %lx\n", offset, Xil_In32(virtOfs));
    return;
  }

  u32 PLReset(){
 //    PSU_Mask_Write(GPIO_MASK_DATA_5_MSW_OFFSET, 0xFFFF0000U, 0x80000000U);
 //    PSU_Mask_Write(GPIO_DIRM_5_OFFSET, 0xFFFFFFFFU, 0x80000000U);
 //    PSU_Mask_Write(GPIO_OEN_5_OFFSET, 0xFFFFFFFFU, 0x80000000U);
 //    PSU_Mask_Write(GPIO_DATA_5_OFFSET, 0xFFFFFFFFU, 0x80000000U);
 //    struct timespec delay;
  // delay.tv_sec = 0;
  // delay.tv_nsec = 1000L;  
  // nanosleep(&delay, NULL);
 //    PSU_Mask_Write(GPIO_DATA_5_OFFSET, 0xFFFFFFFFU, 0x00000000U);
  // delay.tv_sec = 0;
  // delay.tv_nsec = 1000L;  
  // nanosleep(&delay, NULL);
 //    PSU_Mask_Write(GPIO_DATA_5_OFFSET, 0xFFFFFFFFU, 0x80000000U);

 //    return 1;

    int TotalResets = 1;
    int MAX_REG_BITS = 31;
  u32 RegVal = 0;
  u32 MaskVal;

  /* Set EMIO Direction */
  RegVal = Xil_In32(resetHandshakePtr + (GPIO_DIRM_5_OFFSET - RESET_HANDSHAKE_START)) |
    ~(~0U << TotalResets) << (MAX_REG_BITS + 1 - TotalResets);
  Xil_Out32(GPIO_DIRM_5_OFFSET, RegVal);

  /*Assert the EMIO with the required Mask */
  MaskVal = ~(~0U << TotalResets) << (MAX_REG_BITS/2 + 1 - TotalResets) | 0xFFFF0000;
  RegVal = MaskVal & ~(~(~0U << TotalResets) << (MAX_REG_BITS + 1 - TotalResets));
  Xil_Out32(resetHandshakePtr + (GPIO_DATA_5_OFFSET - RESET_HANDSHAKE_START),RegVal);
  mysleep(1000000000L);  

  /*De-assert the EMIO with the required Mask */
  RegVal = ~(~(~0U << TotalResets) << (MAX_REG_BITS + 1 - TotalResets)) & 0xFFFF0000;
  Xil_Out32(resetHandshakePtr + (GPIO_DATA_5_OFFSET - RESET_HANDSHAKE_START), RegVal);
  mysleep(1000000000L);

  /*Assert the EMIO with the required Mask */
  MaskVal = ~(~0U << TotalResets) << (MAX_REG_BITS/2 + 1 - TotalResets) | 0xFFFF0000;
  RegVal = MaskVal & ~(~(~0U << TotalResets) << (MAX_REG_BITS + 1 - TotalResets));
  Xil_Out32(resetHandshakePtr + (GPIO_DATA_5_OFFSET - RESET_HANDSHAKE_START),RegVal);
  mysleep(1000000000L);

  return 1;
  }

  uint64_t getFPGAVirt(uint64_t physAddr) {
    uint32_t offset = physAddr - FRINGE_MEM_BASEADDR;
    return (uint64_t)(fringeMemBase + offset);
  }

  uint64_t getFPGAPhys(uint64_t virtAddr) {
    uint32_t offset = virtAddr - fringeMemBase;
    return (uint64_t)(FRINGE_MEM_BASEADDR + offset);
  }

  virtual void load() {
    std::string cmd = "prog_fpga " + bitfile;
    int dnu = system(cmd.c_str());
    return;
  }

  size_t alignedSize(uint32_t alignment, size_t size) {
    if ((size % alignment) == 0) {
      return size;
    } else {
      return size + alignment - (size % alignment);
    }
  }

  virtual uint64_t malloc(size_t bytes) {

    size_t paddedSize = alignedSize(burstSizeBytes, bytes);

//    int fd = open("/dev/zero", O_RDWR);
//    void *ptr = mmap(0, bytes, PROT_READ|PROT_WRITE|PROT_EXEC, MAP_PRIVATE, fd, 0);
//    close(fd);
//
//    // Lock the page in memory
//    // Do this before writing data to the buffer so that any copy-on-write
//    // mechanisms will give us our own page locked in memory
//    if(mlock(ptr, bytes) == -1) {
//      fprintf(stderr, "Failed to lock page in memory: %s\n", strerror(errno));
//      exit(1);
//    }
//#ifdef USE_PHYS_ADDR
//    uint64_t physAddr = virtToPhys(ptr);
//    physToVirtMap[physAddr] = ptr;
//    return physAddr;
//#else
//    uint64_t addr = (uint64_t)ptr;
//    return addr;
//#endif

    ASSERT(paddedSize <= fpgaFreeMemSize, "FPGA Out-Of-Memory: requested %lu, available %lu\n", paddedSize, fpgaFreeMemSize);

    uint64_t virtAddr = (uint64_t) fpgaMallocPtr;

    for (int i = 0; i < paddedSize / sizeof(u64); i++) {
      u64 *addr = (u64*) (virtAddr + i * sizeof(u64));
      *addr = 4081516 + i;
    }
    fpgaMallocPtr += paddedSize;
    fpgaFreeMemSize -= paddedSize;
    uint64_t physAddr = getFPGAPhys(virtAddr);
    EPRINTF("[malloc] virtAddr = %lx, physAddr = %lx\n", virtAddr, physAddr);
    return physAddr;

  }

  virtual void free(uint64_t buf) {
    EPRINTF("[free] devmem = %lx\n", buf);
  }

  virtual void memcpy(uint64_t devmem, void* hostmem, size_t size) {
    EPRINTF("[memcpy HOST -> FPGA] devmem = %lx, hostmem = %p, size = %lu\n", devmem, hostmem, size);

    void* dst = (void*) getFPGAVirt(devmem);
    std::memcpy(dst, hostmem, size);
   // std::memcpy(dst, hostmem, alignedSize(burstSizeBytes, size));
    // EPRINTF("[Cache Flush] devmem = %lx, size = %u\n", devmem, alignedSize(burstSizeBytes, size));
    // Xil_DCacheFlushRange(devmem, alignedSize(burstSizeBytes, size));

  }

  virtual void memcpy(void* hostmem, uint64_t devmem, size_t size) {

    EPRINTF("[memcpy FPGA -> HOST] hostmem = %p, devmem = %lx, size = %lu\n", hostmem, devmem, size);
    void *src = (void*) getFPGAVirt(devmem);
    std::memcpy(hostmem, src, size);
    //std::memcpy(hostmem, src, alignedSize(burstSizeBytes, size));
    // EPRINTF("[Cache Flush] devmem = %lx, size = %u\n", devmem, alignedSize(burstSizeBytes, size));
    // Xil_DCacheFlushRange(devmem, alignedSize(burstSizeBytes, size));
  }

  void flushCache(uint32_t kb) {
    // Iterate through an array the size of the L2$, to "flush" the cache aka fill it with garbage
    int cacheSizeWords = kb * (1 << 10) / sizeof(int); // 512kB on ZCU, 1MB on ZCU
    int arraySize = cacheSizeWords * 10;
    int *dummyBuf = (int*) std::malloc(arraySize * sizeof(int));
    EPRINTF("[memcpy] dummyBuf = %p, (phys = %lx), arraySize = %d\n", dummyBuf, getFPGAPhys((uint64_t) dummyBuf), arraySize);
    for (int i = 0; i<arraySize; i++) {
      if (i == 0) {
        dummyBuf[i] = 10;
      } else {
        dummyBuf[i] = dummyBuf[i-1] * 2;
      }
    }
    EPRINTF("[memcpy] dummyBuf = %p, dummyBuf[%d] = %d\n", dummyBuf, arraySize-1, dummyBuf[arraySize-1]);
  }

  void dumpRegs() {
    fprintf(stderr, "---- DUMPREGS ----\n");
    for (int i=0; i<100; i++) {
      fprintf(stderr, "reg[%d] = %08lx\n", i, readReg(i));
    }
    fprintf(stderr, "---- END DUMPREGS ----\n");
  }

  void debugs() {
    dumpRegs();
    fprintf(stderr, "---- Let the debugging begin ----\n");

    // Deq the debug FIFO into registers
    for (int i = 0; i < 5; i++) {
      // Pulse deq signal
      writeReg(0+2, 1);
      mysleep(10000000000L);
      writeReg(0+2, 0);

      // Dump regs
      dumpRegs();
    }

    fprintf(stderr, "---- End debugging ----\n");
  }

  void mysleep(unsigned long nanosec) {
    struct timespec delay;
    
    delay.tv_sec = 0;
    delay.tv_nsec = nanosec;  /* Half a second in nano's */
    nanosleep(&delay, NULL);
  }

  virtual void run() {
    EPRINTF("[run] Begin..\n");
     // Current assumption is that the design sets arguments individually
    uint32_t status = 0;
    double timeout = 60; // seconds
    int timed_out = 0;

    // Implement 4-way handshake
    writeReg(statusReg, 0);
    writeReg(commandReg, 2);
    mysleep(1000);
    writeReg(commandReg, 0);
    mysleep(1000);
    // dumpNonDebugRegs();
    // PLReset();
    // dumpNonDebugRegs();
    writeReg(commandReg, 1);

    // fprintf(stderr, "Running design..\n");
    double startTime = getTime();
    int num = 0;
    while((status == 0)) {
      status = readReg(statusReg);
      num++;
      if (num % 10000000 == 0) {
        double endTime = getTime();
        EPRINTF("Elapsed time: %lf ms, status = %08x\n", endTime - startTime, status);
        dumpAllRegs();
        if (endTime - startTime > timeout * 1000) {
          timed_out = 1;
          fprintf(stderr, "TIMEOUT, %lf seconds elapsed..\n", (endTime - startTime) / 1000 );
          break;
        }
      }
    }
    double endTime = getTime();
    fprintf(stderr, "Design done, ran for %lf ms, status = %08x\n", endTime - startTime, status);
    writeReg(commandReg, 0);
    // dumpAllRegs();
    while (status == 1) {
      if (timed_out == 1) {
        break;
      }
      status = readReg(statusReg);
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
  }

  virtual uint64_t getArg64(uint32_t arg, bool isIO) {
    return getArg(arg, isIO);
  }

  virtual uint64_t getArgIn(uint32_t arg, bool isIO) {
    return readReg(2+arg);
  }

  virtual uint64_t getArg(uint32_t arg, bool isIO) {
    if (isIO) {
      return readReg(2+arg);
    } else {
      if (numArgIns == 0) {
        return readReg(1-numArgIOs+2+arg);
      } else {
        return readReg(numArgIns-numArgIOs+2+arg);
      }

    }
  }

  virtual void writeReg(uint32_t reg, uint64_t data) {
    mysleep(100000000L);  /* Half a second in nano's */
    Xil_Out64(fringeScalarBase+reg*sizeof(u64), data);
  }

  virtual uint64_t readReg(uint32_t reg) {
    uint64_t value = Xil_In64(fringeScalarBase+reg*sizeof(u64));
//    fprintf(stderr, "[readReg] Reading register %d, value = %lx\n", reg, value);
    return value;
  }

  void dumpAllRegs() {
    int argIns = numArgIns == 0 ? 1 : numArgIns;
    int argOuts = (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0) ? 1 : numArgOuts;
    int debugRegStart = 2 + argIns + argOuts + numArgOutInstrs + numArgEarlyExits;
    int totalRegs = argIns + argOuts + numArgOutInstrs + numArgEarlyExits + 2 + NUM_DEBUG_SIGNALS;

    for (int i=0; i<totalRegs; i++) {
      uint64_t value = readReg(i);
      if (i < debugRegStart) {
        if (i == 0) EPRINTF(" ******* Non-debug regs *******\n");
        EPRINTF("\tR%d: %016lx (%08lu)\n", i, value, value);
      } else {
        if (i == debugRegStart) EPRINTF("\n\n ******* Debug regs *******\n");
        EPRINTF("\tR%d %s: %016lx (%08lu)\n", i, signalLabels[i - debugRegStart], value, value);
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
//    int numDebugRegs = 224;
    EPRINTF(" ******* Debug regs *******\n");
    int argInOffset = numArgIns == 0 ? 1 : numArgIns;
    int argOutOffset = (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0) ? 1 : numArgOuts;
    EPRINTF("argInOffset: %d\n", argInOffset);
    EPRINTF("argOutOffset: %d\n", argOutOffset);
    for (int i=0; i<NUM_DEBUG_SIGNALS; i++) {
      if (i % 16 == 0) EPRINTF("\n");
      uint64_t value = readReg(argInOffset + argOutOffset + numArgOutInstrs + numArgEarlyExits + 2 + i);
      EPRINTF("\t%s: %016lx (%08lu)\n", signalLabels[i], value, value);
    }
    EPRINTF(" **************************\n");
  }

  ~FringeContextZCU() {
    dumpDebugRegs();
  }
};

// Fringe Simulation APIs
void fringeInit(int argc, char **argv) {
}
#endif
