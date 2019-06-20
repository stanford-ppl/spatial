#ifndef __KCU1500_ADDRESS_MAP_H__
#define __KCU1500_ADDRESS_MAP_H__

#include <stdio.h>
#include <assert.h>
#include <stdlib.h>
#include <unistd.h>

// Memory mapping related constants
#define FRINGE_MEM_BASEADDR     0x20000000
#define FRINGE_SCALAR_BASEADDR  0x400000000
#define MEM_SIZE                0x20000000
#define MAP_LEN                 0x10000

// typedef unsigned long u32;

// Power-up Request Status for PL
#define RESET_HANDSHAKE_SIZE		  0x010000
#define RESET_HANDSHAKE_START       0xFF0A0000
#define GPIO_MASK_DATA_5_MSW_OFFSET 0XFF0A0028
#undef GPIO_DIRM_5_OFFSET 
#define GPIO_DIRM_5_OFFSET          0XFF0A0340
#undef GPIO_OEN_5_OFFSET 
#define GPIO_OEN_5_OFFSET           0XFF0A0348
#undef GPIO_DATA_5_OFFSET 
#define GPIO_DATA_5_OFFSET          0XFF0A0054

// Bit masks and positions - Command register
#define MREAD(val, mask) (((val) & (mask)) >> __builtin_ctz(mask))
#define MWRITE(val, mask) (((val) << __builtin_ctz(mask)) & (mask))

// Some helper macros
#define EPRINTF(...) fprintf(stderr, __VA_ARGS__)
#define ASSERT(cond, ...) \
  if (!(cond)) { \
    EPRINTF("\n");        \
    EPRINTF(__VA_ARGS__); \
    EPRINTF("\n");        \
    EPRINTF("Assertion (%s) failed in %s, %d\n", #cond, __FILE__, __LINE__); \
    assert(0);  \
  }

#endif  // ifndef __KCU1500_ADDRESS_MAP_H__
