#ifndef __ZYNQ_ADDRESS_MAP_H__
#define __ZYNQ_ADDRESS_MAP_H__

#include <stdio.h>
#include <assert.h>
#include <stdlib.h>
#include <unistd.h>

// Memory mapping related constants
#define MAP_LEN                 0x10000
#define FRINGE_SCALAR_BASEADDR  0x40000000

typedef unsigned long u32;


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

#endif  // ifndef __ZYNQ_ADDRESS_MAP_H__
