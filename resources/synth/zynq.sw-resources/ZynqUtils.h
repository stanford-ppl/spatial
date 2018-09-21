#ifndef __ZYNQ_UTILS_H__
#define __ZYNQ_UTILS_H__

#include <arpa/inet.h>
#include <time.h>
#include <stdio.h>
#include <assert.h>
#include <stdlib.h>
#include <unistd.h>

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

struct opts {
  int sobelMode;
  int webcam;
  char *videoFile;
  char *instBuf;
  char *inBuf;
  char *outBuf;
  int debug;
};


int fileToBuf(unsigned char *buf, char *filename, u32 max_bytes);
int fileToBufHex(unsigned char *buf, char *filename, u32 max_bytes);
void bufToFile(char *filename, unsigned char *buf, u32 max_bytes);
void bufToFileHex(char *filename, unsigned char *buf, u32 max_bytes);
void htonlBuf(uint32_t *buf, u32 numWords);
void ntohlBuf(uint32_t *buf, u32 numWords);
void printHelp(int argc, char **argv);
void printOpts();
void parseOpts(int argc, char **argv, struct opts *opts);
u32 Xil_In32(u32 Addr);
void Xil_Out32(u32 OutAddress, u32 Value);
double getTime();

#endif  // ifndef __ZYNQ_UTILS_H__
