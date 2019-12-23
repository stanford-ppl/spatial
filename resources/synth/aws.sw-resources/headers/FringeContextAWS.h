#ifndef __FRINGE_CONTEXT_AWS_H__
#define __FRINGE_CONTEXT_AWS_H__

#include <assert.h>
#include "FringeContextBase.h"
//#include "commonDefs.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#define UINT64_C_AWS(c) c ## ULL

#ifndef EPRINTF
#define EPRINTF(...) fprintf(stderr, __VA_ARGS__)
#endif

using namespace std;

#ifndef ASSERT
#define ASSERT(cond, ...) \
  if (!(cond)) { \
    EPRINTF("\n");        \
    EPRINTF(__VA_ARGS__); \
    EPRINTF("\n");        \
    EPRINTF("Assertion (%s) failed in %s, %d\n", #cond, __FILE__, __LINE__); \
    assert(0);  \
  }

struct opts {
  int sobelMode;
  int webcam;
  char *videoFile;
  char *instBuf;
  char *inBuf;
  char *outBuf;
  int debug;
};
#endif

#ifdef SIM // Sim
  // Unfortunately Sim seems to have changed in the new shell. If this doesn't work 
  // then see how cl_hello_world and cl_dram_dma do simulation (new APIs) and change this file.  
  #include "sh_dpi_tasks.h"
  // #include <utils/sh_dpi_tasks.h>  Try this if needed
  #define BASE_ADDR_A         UINT64_C_AWS(0x0000000000000100)   // DDR CHANNEL A
  #define BASE_ADDR_B         UINT64_C_AWS(0x0000000000000200)   // DDR CHANNEL B
  #define BASE_ADDR_C         UINT64_C_AWS(0x0000000000000300)   // DDR CHANNEL C
  #define BASE_ADDR_D         UINT64_C_AWS(0x0000000000000400)   // DDR CHANNEL D
  #define ATG                 UINT64_C_AWS(0x30)
#else // F1
  #include <fcntl.h>    // Probably don't need most of these headers
  #include <errno.h>
  #include <string.h>
  #include <unistd.h>
  #include <poll.h>
  #include <stdbool.h>
  #include <stdarg.h>
  #include <fpga_pci.h>
  #include <fpga_mgmt.h>
  #include <utils/lcd.h>
  #include <time.h>
  #include <sys/stat.h>
  #include <sys/types.h>

  // Try these if needed
  // #include "fpga_pci.h"
  // #include "fpga_mgmt.h"
  #include <fpga_dma.h>
  // #include "utils/lcd.h"
  
  #define MEM_16G (1ULL << 34)
  static const uint16_t AMZ_PCI_VENDOR_ID = 0x1D0F; /* Amazon PCI Vendor ID */
  static const uint16_t PCI_DEVICE_ID = 0xF001;

  #define LOW_32b(a)  ((uint32_t)((uint64_t)(a) & 0xffffffff))
  #define HIGH_32b(a) ((uint32_t)(((uint64_t)(a)) >> 32L))
#endif // F1

#define SCALAR_CMD_BASE_ADDR   UINT64_C_AWS(0x1000000)
#define SCALAR_IN_BASE_ADDR    UINT64_C_AWS(0x1010000)
#define SCALAR_OUT_BASE_ADDR   UINT64_C_AWS(0x1080000)

#define SCALAR_ARG_INCREMENT   UINT64_C_AWS(0x40)

#define CMD_REG_ADDR           UINT64_C_AWS(0x00)
#define STATUS_REG_ADDR        UINT64_C_AWS(0x20)
// #define DDR_STATUS_REG_ADDR    UINT64_C_AWS(0x40)
#define PERF_COUNTER           UINT64_C_AWS(0x40)
#define RESET_REG_ADDR         UINT64_C_AWS(0x60)

class FringeContextAWS : public FringeContextBase<void> {

private:
#ifdef SIM
#else // F1
  int read_fd;
  int write_fd;
  int slot_id;
  int channel;
  pci_bar_handle_t pci_bar_handle;
#endif // F1

  int numArgIns = 0;
  int numArgOuts = 0;
  int numArgIOs = 0;
  int numArgOutInstrs = 0;
  int numArgEarlyExits = 0;

  // Helper to peek in sim or F1
  void aws_peek(uint64_t addr, uint32_t *value) {
#ifdef SIM
    cl_peek(addr, value);
#else // F1
    fpga_pci_peek(pci_bar_handle, addr, value);
#endif // F1
  }


  // Helper to poke in sim or F1
  void aws_poke(uint64_t addr, uint32_t  value) {
#ifdef SIM
    cl_poke(addr, value);
#else // F1
    fpga_pci_poke(pci_bar_handle, addr, value);
#endif // F1
  }


#ifdef SIM
#else // F1
  // Check function from Amazon cl_dram_dma example
  int check_slot_config(int slot_id) {
    int rc;
    struct fpga_mgmt_image_info info = {0};

    /* get local image description, contains status, vendor id, and device id */
    rc = fpga_mgmt_describe_local_image(slot_id, &info, 0);
    fail_on(rc, out, "Unable to get local image information. Are you running as root?");

    /* check to see if the slot is ready */
    if (info.status != FPGA_STATUS_LOADED) {
      rc = 1;
      fail_on(rc, out, "Slot %d is not ready", slot_id);
    }

    /* confirm that the AFI that we expect is in fact loaded */
    if (info.spec.map[FPGA_APP_PF].vendor_id != AMZ_PCI_VENDOR_ID ||
        info.spec.map[FPGA_APP_PF].device_id != PCI_DEVICE_ID) {
      rc = 1;
      printf("The slot appears loaded, but the pci vendor or device ID doesn't "
             "match the expected values. You may need to rescan the fpga with \n"
             "fpga-describe-local-image -S %i -R\n"
             "Note that rescanning can change which device file in /dev/ a FPGA will map to.\n"
             "To remove and re-add your xdma driver and reset the device file mappings, run\n"
             "`sudo rmmod xdma && sudo insmod <aws-fpga>/sdk/linux_kernel_drivers/xdma/xdma.ko`\n",
             slot_id);
      fail_on(rc, out, "The PCI vendor id and device of the loaded image are "
                       "not the expected values.");
    }

  out:
    return rc;
  }
#endif // F1

public:
  
  FringeContextAWS(string path = "") : FringeContextBase(path) {
#ifdef SIM
#else // F1
    slot_id = 0; // For now fix slot to 0
    channel = 0; // For now fix channel to 0
    read_fd = -1;
    write_fd = -1;

    /* pci_bar_handle_t is a handler for an address space exposed by one PCI BAR on one of the PCI PFs of the FPGA */
    pci_bar_handle = PCI_BAR_HANDLE_INIT;
#endif // F1
  }

  virtual void load() {
    aws_poke(SCALAR_CMD_BASE_ADDR + RESET_REG_ADDR, 1);
    aws_poke(SCALAR_CMD_BASE_ADDR + RESET_REG_ADDR, 0);

#ifdef SIM
    // Nothing needed for sim

#else // F1

    // TODO: load using
    //  fpga_mgmt_load_local_image,
    // or just use system() to run:
    //  sudo fpga-load-local-image -S 0 -I agfi-...
    
    // TODO: set slot_id based on constructor path or input arg to this load()
    
    int pf_id = 0;
    int bar_id = 0;
    int fpga_attach_flags = 0;
    int rc;
    fpga_mgmt_init();
    
    // ---------------------------------
    // PCIe
    // ---------------------------------
    
    rc = fpga_pci_init();
    fail_on(rc, out, "Unable to initialize the fpga_pci library");
    // rc = check_afi_ready(slot_id);
    // fail_on(rc, out, "AFI not ready");

    /* attach to the fpga, with a pci_bar_handle out param
     * To attach to multiple slots or BARs, call this function multiple times,
     * saving the pci_bar_handle to specify which address space to interact with in
     * other API calls.
     * This function accepts the slot_id, physical function, and bar number
     */

    // make sure the AFI is loaded and ready
    rc = check_slot_config(slot_id);
    fail_on(rc, out, "slot config is not correct");

    rc = fpga_pci_attach(slot_id, pf_id, bar_id, fpga_attach_flags, &pci_bar_handle);
    fail_on(rc, out, "Unable to attach to the AFI on slot id %d", slot_id);

    // ---------------------------------
    // XDMA
    // ---------------------------------
    
    read_fd = fpga_dma_open_queue(FPGA_DMA_XDMA, slot_id,
        /*channel*/ 0, /*is_read*/ true);
    fail_on((rc = (read_fd < 0) ? -1 : 0), out, "unable to open read dma queue");

    write_fd = fpga_dma_open_queue(FPGA_DMA_XDMA, slot_id,
        /*channel*/ 0, /*is_read*/ false);
    fail_on((rc = (write_fd < 0) ? -1 : 0), out, "unable to open write dma queue");
    
  out:
    ;
    // does nothing

#endif // F1
  }


  // Close DMA file descriptor and PCI BAR handle
  ~FringeContextAWS() {
#ifdef SIM
#else // F1
    if (write_fd >= 0) {
        close(write_fd);
    }
    if (read_fd >= 0) {
        close(read_fd);
    }
    if (pci_bar_handle >= 0) {
        int rc = fpga_pci_detach(pci_bar_handle);
        if (rc) {
            printf("Failure while detaching from the fpga.\n");
        }
    }
#endif // F1
  }


  // Get pointer to device memory
  virtual uint64_t malloc(size_t bytes) {
    uint64_t return_ptr = NULL;
    int nbursts;
    static uint64_t current_heap_ptr = UINT64_C_AWS(0);
    nbursts = (bytes + 63) / 64;
    return_ptr = ((uint64_t)(current_heap_ptr));
    current_heap_ptr = ((uint64_t)(current_heap_ptr)) + 4*16*nbursts;
    printf("Adjusted current_heap_ptr to %d\n", current_heap_ptr);
    return return_ptr;
  }


  // Unimplemented
  virtual void free(uint64_t buf) {
    printf("Warning: free() not implemented\n");
  }


  // Copy host to device
  virtual void memcpy(uint64_t devmem, void* hostmem, size_t size) {
    printf("[memcpy HOST->DEV] hostmem = %p, devmem = %lx, size = %lx\n", hostmem, devmem, size);
#ifdef SIM
    TMP_que_buffer_to_cl((uint64_t)hostmem, devmem, size);
    TMP_start_que_to_cl();
    /*
    int timeout_count = 0;
    uint32_t read_data;
    do {
      TMP_is_dma_to_cl_done(&read_data);
      read_data &= 0x00000001;
      sv_pause(10); // ns
      timeout_count++;
    } while ((read_data != 1) && (timeout_count < 500));
    */
    sv_pause(10000); // needed because 'is...done' does not poll bvalid/bready, but only that the cmd is queued (and only 1 burst)
#else // F1
    int rc = 0;
    char *write_buffer = (char *)hostmem;
    size_t write_offset = 0;
    while (write_offset < size) {
      /*
      if (write_offset != 0) {
        printf("Partial write by driver, trying again with remainder of buffer (%lu bytes)\n", size - write_offset);
      }
      */
      size_t count = size - write_offset;
      if (count > 128) {
        count = 128;
      }
      rc = pwrite(write_fd, write_buffer + write_offset, count, channel*MEM_16G + devmem + write_offset);
      assert(rc >= 0);
      write_offset += rc;
    }
#endif // F1
  }


  // Copy device to host
  virtual void memcpy(void* hostmem, uint64_t devmem, size_t size) {
    printf("[memcpy DEV->HOST] hostmem = %p, devmem = %lx, size = %lx\n", hostmem, devmem, size);
#ifdef SIM
    TMP_que_cl_to_buffer((uint64_t)hostmem, devmem, size);
    TMP_start_que_to_buffer();
    /*
    int timeout_count = 0;
    uint32_t read_data;
    do {
      TMP_is_dma_to_buffer_done(&read_data);
      read_data &= 0x00000001;
      sv_pause(10); // ns
      timeout_count++;
    } while ((read_data != 1) && (timeout_count < 500));
    */
    sv_pause(10000); // needed because 'is...done' does not poll read done, only queued (and only 1 burst)
#else // F1
    int rc = 0;
    char *read_buffer = (char *)hostmem;
    size_t read_offset = 0;
    while (read_offset < size) {
      /*
      if (read_offset != 0) {
        printf("Partial read by driver, trying again with remainder of buffer (%lu bytes)\n", size - read_offset);
      }
      */
      size_t count = size - read_offset;
      if (count > 128) {
        count = 128;
      }
      rc = pread(read_fd, read_buffer + read_offset, count, channel*MEM_16G + devmem + read_offset);
      assert(rc >= 0);
      read_offset += rc;
    }
#endif // F1
  }


  // set enable high in app and poll until done is high
  virtual void run() {
    // TODO: See if these lines should be here rather than in load()
    aws_poke(SCALAR_CMD_BASE_ADDR + RESET_REG_ADDR, 1);
    aws_poke(SCALAR_CMD_BASE_ADDR + RESET_REG_ADDR, 0);

    printf("[run] Begin\n");
#ifdef SIM
    // These may not be needed anymore
    aws_poke(BASE_ADDR_A + ATG, 0x00000001);
    aws_poke(BASE_ADDR_B + ATG, 0x00000001);
    aws_poke(BASE_ADDR_C + ATG, 0x00000001);
    aws_poke(BASE_ADDR_D + ATG, 0x00000001);
#else // F1
    double startTime = 0.0;
    struct timespec ts1;
    clock_gettime (CLOCK_MONOTONIC, &ts1);
    startTime = (double) (ts1.tv_sec);
    startTime = (double) (startTime * 1000 + (double)(ts1.tv_nsec) / 1000000) ;
#endif // F1
    // aws_poke(BASE_ADDR + NUM_INST, 0x00000000);  // TODO: Move outside run()?
    uint32_t status;
    aws_poke(SCALAR_CMD_BASE_ADDR + CMD_REG_ADDR, 1);
    do {
      aws_peek(SCALAR_CMD_BASE_ADDR + STATUS_REG_ADDR, &status);
    } while (!status);
    // De-assert enable?
#ifdef SIM
    // These may not be needed anymore
    aws_poke(BASE_ADDR_A + ATG, 0x00000000);
    aws_poke(BASE_ADDR_B + ATG, 0x00000000);
    aws_poke(BASE_ADDR_C + ATG, 0x00000000);
    aws_poke(BASE_ADDR_D + ATG, 0x00000000);
#else // F1
    double endTime = 0.0;
    struct timespec ts2;
    clock_gettime (CLOCK_MONOTONIC, &ts2);
    endTime = (double) (ts2.tv_sec);
    endTime = (double) (endTime * 1000 + (double)(ts2.tv_nsec) / 1000000) ;
    printf("Design ran for %lf ms\n", endTime - startTime);
    // /*
    uint32_t total_cycles;
    aws_peek(SCALAR_CMD_BASE_ADDR + PERF_COUNTER, &total_cycles);
    printf("Total cycles = %d\n", total_cycles);
    // */
#endif // F1
    printf("[run] Done\n");
  }


  // write 64b scalar
  virtual void setArg(uint32_t arg, uint64_t data, bool isIO) {
    uint32_t value_32b;
    
    value_32b = LOW_32b(data);
    aws_poke(SCALAR_IN_BASE_ADDR  + SCALAR_ARG_INCREMENT*arg,                      value_32b);

    value_32b = HIGH_32b(data);
    aws_poke(SCALAR_IN_BASE_ADDR  + SCALAR_ARG_INCREMENT*arg + UINT64_C_AWS(0x20), value_32b);
  }


  // read 64b scalar
  virtual uint64_t getArg64(uint32_t arg, bool isIO) {
    return getArg(arg, isIO);
  }

  virtual uint64_t getArg(uint32_t arg, bool isIO) {
    uint64_t value;
    uint32_t value_32b;
    
    aws_peek(SCALAR_OUT_BASE_ADDR + SCALAR_ARG_INCREMENT*(arg - numArgIns),                      &value_32b);
    value = (uint64_t)(value_32b);
    
    aws_peek(SCALAR_OUT_BASE_ADDR + SCALAR_ARG_INCREMENT*(arg - numArgIns) + UINT64_C_AWS(0x20), &value_32b);
    value = value | (uint64_t)((uint64_t)(value_32b) << 32);
    
    return value;
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

  void flushCache(uint32_t kb) {
    // // Iterate through an array the size of the L2$, to "flush" the cache aka fill it with garbage
    // int cacheSizeWords = kb * (1 << 10) / sizeof(int); // 512kB on Zynq, 1MB on ZCU
    // int arraySize = cacheSizeWords * 10;
    // int *dummyBuf = (int*) malloc(arraySize * sizeof(int));
    // EPRINTF("[memcpy] dummyBuf = %p, (phys = %lx), arraySize = %d\n", dummyBuf, getFPGAPhys((uint64_t) dummyBuf), arraySize);
    // for (int i = 0; i<arraySize; i++) {
    //   if (i == 0) {
    //     dummyBuf[i] = 10;
    //   } else {
    //     dummyBuf[i] = dummyBuf[i-1] * 2;
    //   }
    // }
    // EPRINTF("[memcpy] dummyBuf = %p, dummyBuf[%d] = %d\n", dummyBuf, arraySize-1, dummyBuf[arraySize-1]);
  }

  virtual void setNumArgOutInstrs(uint32_t number) {
    numArgOutInstrs = number;
  }

  // Unimplemented
  virtual void writeReg(uint32_t reg, uint64_t data) {
    assert(false);
  }


  // Unimplemented
  virtual uint64_t readReg(uint32_t reg) {
    assert(false);
    return NULL;
  }

};

// Fringe Simulation APIs
void fringeInit(int argc, char **argv) {
}

#endif
