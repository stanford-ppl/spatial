#ifndef __FRINGE_CONTEXT_DE1_H__
#define __FRINGE_CONTEXT_DE1_H__

#include "DE1AddressMap.h"
#include "DE1Utils.h"
#include "FringeContextBase.h"
#include "generated_debugRegs.h"
#include <cstring>
#include <errno.h>
#include <fcntl.h>
#include <map>
#include <stdlib.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>
#include <list>
#include <iostream>
// #include <xil_cache.h>
// #include <xil_io.h>

// Some key code snippets have been borrowed from the following source:
// https://shanetully.com/2014/12/translating-virtual-addresses-to-physcial-addresses-in-user-space

// The page frame shifted left by PAGE_SHIFT will give us the physcial address
// of the frame
// // Note that this number is architecture dependent. For me on x86_64 with
// 4096 page sizes,
// // it is defined as 12. If you're running something different, check the
// kernel source
// // for what it is defined as.
#define PAGE_SHIFT 12
#define PAGEMAP_LENGTH 8
#define USE_PHYS_ADDR

using namespace std;

/**
 * DE1 Fringe Context
 */

typedef long unsigned int luint;
typedef volatile unsigned int vuint;

class FringeContextDE1 : public FringeContextBase<void>
{

    uint32_t burstSizeBytes;
    int fd;
    vuint *fringeScalarBase;
    vuint *fringeMemBase;
    vuint *fpgaMallocPtr;
    u32 fpgaFreeMemSize;
    u32 commandReg;
    u32 statusReg;

    map<uint32_t, void *> physToVirtMap;

    void *physToVirt(uint32_t physAddr)
    {
        map<uint32_t, void *>::iterator iter = physToVirtMap.find(physAddr);
        if (iter == physToVirtMap.end())
        {
            EPRINTF("Physical address '%lx' not found in physToVirtMap\n. Was this "
                    "allocated before?",
                    (long unsigned int)physAddr);
            exit(-1);
        }
        return iter->second;
    }

    uint32_t virtToPhys(void *virt)
    {
        uint32_t phys = 0;

        // Open the pagemap file for the current process
        FILE *pagemap = fopen("/proc/self/pagemap", "rb");
        FILE *origmap = pagemap;

        // Seek to the page that the buffer is on
        unsigned long offset = (unsigned long)virt / getpagesize() * PAGEMAP_LENGTH;
        if (fseek(pagemap, (unsigned long)offset, SEEK_SET) != 0)
        {
            fprintf(stderr, "Failed to seek pagemap to proper location\n");
            exit(1);
        }

        // The page frame number is in bits 0-54 so read the first 7 bytes and clear
        // the 55th bit
        unsigned long page_frame_number = 0;
        fread(&page_frame_number, 1, PAGEMAP_LENGTH - 1, pagemap);

        page_frame_number &= 0x7FFFFFFFFFFFFF;

        fclose(origmap);

        // Find the difference from the virt to the page boundary
        unsigned int distance_from_page_boundary =
            (unsigned long)virt % getpagesize();
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
    string bitfile;

    FringeContextDE1(string path = "") : FringeContextBase(path)
    {
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

        int retval = setuid(0);
        ASSERT(retval == 0, "setuid(0) failed\n");
        fd = open("/dev/mem", O_RDWR);
        if (fd < 1)
        {
            perror("error opening /dev/mem\n");
        }

        // Initialize pointers to fringeScalarBase
        vuint *ptr = (vuint *)mmap(
            NULL, MAP_LEN, PROT_READ | PROT_WRITE, MAP_SHARED, fd,
            FRINGE_SCALAR_BASEADDR);
        fringeScalarBase = ptr;
        EPRINTF("placing fringeScalarBase at %lx\n", (luint)fringeScalarBase);

        // Initialize pointer to fringeMemBase
        fringeMemBase = (vuint *)mmap(
            NULL, MEM_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, fd,
            FRINGE_M_AXI_BASEADDR);
        EPRINTF("placing fringeMemBase at %lx\n", (luint)fringeMemBase);
        fpgaMallocPtr = fringeMemBase;
    }

    uint32_t getFPGAVirt(uint32_t physAddr)
    {
        uint32_t offset = physAddr - FRINGE_MEM_BASEADDR;
        return (uint32_t)(fringeMemBase + offset);
    }

    uint32_t getFPGAPhys(uint32_t virtAddr)
    {
        uint32_t offset = virtAddr - (uint32_t)fringeMemBase;
        return (uint32_t)(FRINGE_MEM_BASEADDR + offset);
    }

    virtual void load()
    {
        // Commands for writing fpga to DE1
        list<string> cmds = {
            "echo 0 > /sys/class/fpga-bridge/fpga2hps/enable",
            "echo 0 > /sys/class/fpga-bridge/hps2fpga/enable",
            "echo 0 > /sys/class/fpga-bridge/lwhps2fpga/enable",
            "dd if=SpatialIP.rbf of=/dev/fpga0 bs=1M",
            "echo 1 > /sys/class/fpga-bridge/fpga2hps/enable",
            "echo 1 > /sys/class/fpga-bridge/hps2fpga/enable",
            "echo 1 > /sys/class/fpga-bridge/lwhps2fpga/enable"};
        list<string>::iterator it;
        int dnu = -1;
        for (it = cmds.begin(); it != cmds.end(); ++it)
        {
            cout << it->c_str() << endl;
            sleep(0.5);
            dnu = system(it->c_str());
        }

        return;
    }

    size_t alignedSize(uint32_t alignment, size_t size)
    {
        if ((size % alignment) == 0)
        {
            return size;
        }
        else
        {
            return size + alignment - (size % alignment);
        }
    }

    virtual uint32_t malloc(size_t bytes)
    {

        size_t paddedSize = alignedSize(burstSizeBytes, bytes);
        ASSERT(paddedSize <= fpgaFreeMemSize,
               "FPGA Out-Of-Memory: requested %lu, available %lu\n", (luint)paddedSize,
               (luint)fpgaFreeMemSize);

        uint32_t virtAddr = (uint32_t)fpgaMallocPtr;

        for (int i = 0; i < paddedSize / sizeof(u32); i++)
        {
            u32 *addr = (u32 *)(virtAddr + i * sizeof(u32));
            *addr = 0;
        }
        fpgaMallocPtr += paddedSize;
        fpgaFreeMemSize -= paddedSize;
        uint32_t physAddr = getFPGAPhys(virtAddr);
        EPRINTF("[malloc] virtAddr = %lx, physAddr = %lx\n", (luint)virtAddr, (luint)physAddr);
        return physAddr;
    }

    virtual void free(uint32_t buf) { EPRINTF("[free] devmem = %lx\n", (luint)buf); }

    virtual void memcpy(uint32_t devmem, void *hostmem, size_t size)
    {
        EPRINTF("[memcpy HOST -> FPGA] devmem = %lx, hostmem = %p, size = %lu\n",
                (luint)devmem, hostmem, (luint)size);

        void *dst = (void *)getFPGAVirt(devmem);
        std::memcpy(dst, hostmem, size);
    }

    virtual void memcpy(void *hostmem, uint32_t devmem, size_t size)
    {

        EPRINTF("[memcpy FPGA -> HOST] hostmem = %p, devmem = %lx, size = %lu\n",
                hostmem, (long unsigned int)devmem, (long unsigned int)size);
        void *src = (void *)getFPGAVirt(devmem);
        std::memcpy(hostmem, src, size);
    }

    void flushCache(uint32_t kb)
    {
        int cacheSizeWords =
            kb * (1 << 10) / sizeof(int); // 512kB on DE1, 1MB on DE1
        int arraySize = cacheSizeWords * 10;
        int *dummyBuf = (int *)std::malloc(arraySize * sizeof(int));
        EPRINTF("[memcpy] dummyBuf = %p, (phys = %lx), arraySize = %d\n", (void *)dummyBuf,
                (long unsigned int)getFPGAPhys((uint32_t)dummyBuf), arraySize);
        for (int i = 0; i < arraySize; i++)
        {
            if (i == 0)
            {
                dummyBuf[i] = 10;
            }
            else
            {
                dummyBuf[i] = dummyBuf[i - 1] * 2;
            }
        }
        EPRINTF("[memcpy] dummyBuf = %p, dummyBuf[%d] = %d\n", dummyBuf,
                arraySize - 1, dummyBuf[arraySize - 1]);
    }

    void dumpRegs()
    {
        fprintf(stderr, "---- DUMPREGS ----\n");
        for (int i = 0; i < 100; i++)
        {
            fprintf(stderr, "reg[%d] = %08lx\n", i, (long unsigned int)readReg(i));
        }
        fprintf(stderr, "---- END DUMPREGS ----\n");
    }

    void debugs()
    {
        dumpRegs();
        fprintf(stderr, "---- Let the debugging begin ----\n");

        // Deq the debug FIFO into registers
        for (int i = 0; i < 5; i++)
        {
            // Pulse deq signal
            writeReg(0 + 2, 1);
            mysleep(10000000000L);
            writeReg(0 + 2, 0);

            // Dump regs
            dumpRegs();
        }

        fprintf(stderr, "---- End debugging ----\n");
    }

    void mysleep(unsigned long nanosec)
    {
        struct timespec delay;

        delay.tv_sec = 0;
        delay.tv_nsec = nanosec; /* Half a second in nano's */
        nanosleep(&delay, NULL);
    }

    virtual void run()
    {
        EPRINTF("[run] Begin..\n");
        uint32_t status = 0;
        double timeout = 10; // seconds
        int timed_out = 0;

        // Implement 4-way handshake
        writeReg(statusReg, 0);
        writeReg(commandReg, 2);
        mysleep(1000);
        writeReg(commandReg, 0);
        mysleep(1000);
        writeReg(commandReg, 1);

        double startTime = getTime();
        int num = 0;
        while ((status == 0))
        {
            status = readReg(statusReg);
            num++;
            if (num % 10000000 == 0)
            {
                double endTime = getTime();
                EPRINTF("Elapsed time: %lf ms, status = %08x\n", endTime - startTime,
                        (unsigned int)status);
                dumpAllRegs();
                if (endTime - startTime > timeout * 1000)
                {
                    timed_out = 1;
                    fprintf(stderr, "TIMEOUT, %lf seconds elapsed..\n",
                            (endTime - startTime) / 1000);
                    break;
                }
            }
        }

        double endTime = getTime();
        fprintf(stderr, "Design done, ran for %lf ms, status = %08x\n",
                endTime - startTime, status);
        writeReg(commandReg, 0);
        // dumpAllRegs();
        while (status == 1)
        {
            if (timed_out == 1)
            {
                break;
            }
            status = readReg(statusReg);
        }
    }

    virtual void setNumArgIns(uint32_t number) { numArgIns = number; }

    virtual void setNumEarlyExits(uint32_t number) { numArgEarlyExits = number; }

    virtual void setNumArgIOs(uint32_t number) { numArgIOs = number; }

    virtual void setNumArgOuts(uint32_t number) { numArgOuts = number; }

    virtual void setNumArgOutInstrs(uint32_t number) { numArgOutInstrs = number; }

    virtual void setArg(uint32_t arg, uint32_t data, bool isIO)
    {
        writeReg(arg + 2, data);
    }

    virtual uint32_t getArg64(uint32_t arg, bool isIO)
    {
        return getArg(arg, isIO);
    }

    virtual uint32_t getArgIn(uint32_t arg, bool isIO)
    {
        return readReg(2 + arg);
    }

    virtual uint32_t getArg(uint32_t arg, bool isIO)
    {
        if (numArgIns == 0)
            return readReg(3 + arg);
        else
            return readReg(2 + arg);
    }

    virtual void writeReg(uint32_t reg, uint32_t data)
    {
        *(fringeScalarBase + reg) = data;
    }

    virtual uint32_t readReg(uint32_t reg)
    {
        uint32_t value = *(fringeScalarBase + reg);
        return value;
    }

    void dumpAllRegs()
    {
        int argIns = numArgIns == 0 ? 1 : numArgIns;
        int argOuts =
            (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0)
                ? 1
                : numArgOuts;
        int debugRegStart =
            2 + argIns + argOuts + numArgOutInstrs + numArgEarlyExits;
        int totalRegs = argIns + argOuts + numArgOutInstrs + numArgEarlyExits + 2 +
                        NUM_DEBUG_SIGNALS;

        for (int i = 0; i < totalRegs; i++)
        {
            uint32_t value = readReg(i);
            if (i < debugRegStart)
            {
                if (i == 0)
                    EPRINTF(" ******* Non-debug regs *******\n");
                EPRINTF("\tR%d: %016lx (%08lu)\n", i, (luint)value, (luint)value);
            }
            else
            {
                if (i == debugRegStart)
                    EPRINTF("\n\n ******* Debug regs *******\n");
                EPRINTF("\tR%d %s: %016lx (%08lu)\n", i,
                        signalLabels[i - debugRegStart], (luint)value, (luint)value);
            }
        }
    }

    void dumpNonDebugRegs()
    {
        int argIns = numArgIns == 0 ? 1 : numArgIns;
        int argOuts =
            (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0)
                ? 1
                : numArgOuts;
        int debugRegStart =
            2 + argIns + argOuts + numArgOutInstrs + numArgEarlyExits;

        for (int i = 0; i < debugRegStart; i++)
        {
            uint32_t value = readReg(i);
            if (i < debugRegStart)
            {
                if (i == 0)
                    EPRINTF(" ******* Non-debug regs *******\n");
                EPRINTF("\tR%d: %016lx (%08lu)\n", i, (luint)value, (luint)value);
            }
        }
    }

    void dumpDebugRegs()
    {
        //    int numDebugRegs = 224;
        EPRINTF(" ******* Debug regs *******\n");
        int argInOffset = numArgIns == 0 ? 1 : numArgIns;
        int argOutOffset =
            (numArgOuts == 0 & numArgOutInstrs == 0 & numArgEarlyExits == 0)
                ? 1
                : numArgOuts;
        EPRINTF("argInOffset: %d\n", argInOffset);
        EPRINTF("argOutOffset: %d\n", argOutOffset);
        for (int i = 0; i < NUM_DEBUG_SIGNALS; i++)
        {
            if (i % 16 == 0)
                EPRINTF("\n");
            uint32_t value = readReg(argInOffset + argOutOffset + numArgOutInstrs +
                                     numArgEarlyExits + 2 + i);
            EPRINTF("\t%s: %016lx (%08lu)\n", signalLabels[i], (luint)value, (luint)value);
        }
        EPRINTF(" **************************\n");
    }

    ~FringeContextDE1() { dumpDebugRegs(); }
};

// Fringe Simulation APIs
void fringeInit(int argc, char **argv) {}

#endif
