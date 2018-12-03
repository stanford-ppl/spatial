#include <string.h>
#include <ctype.h>
#include "ZynqUtils.h"

u32 Xil_In32(u32 Addr)
{
	return *(volatile u32 *) Addr;
}

void Xil_Out32(u32 OutAddress, u32 Value)
{
	*(volatile u32 *) OutAddress = Value;
}


int fileToBuf(unsigned char *buf, char *filename, u32 max_bytes)
{
  ASSERT(buf, "buf is NULL");
  ASSERT(filename, "filename is NULL");
  ASSERT(max_bytes > 0, "Trying to read invalid (%lu) number of bytes from %s\n", max_bytes, filename);

  FILE *ifile = fopen(filename, "rb");
  int bytesRead = 0;
  ASSERT(ifile, "Could not open file %s for reading\n", filename);
  bytesRead = fread(buf, sizeof(char), max_bytes, ifile);
  ASSERT(bytesRead > 0, "Read invalid (%d) number of bytes from %s\n", bytesRead, filename);
  fclose(ifile);
  return bytesRead;
}

void htonlBuf(uint32_t *buf, u32 numWords)
{
  ASSERT(buf, "Buf passed to ntohl_buf is NULL!\n");
  for (u32 i = 0; i<numWords; i++) {
    buf[i] = htonl(buf[i]);
  }
}


void ntohlBuf(uint32_t *buf, u32 numWords)
{
  ASSERT(buf, "Buf passed to ntohl_buf is NULL!\n");
  for (u32 i = 0; i<numWords; i++) {
    buf[i] = ntohl(buf[i]);
  }
}

int fileToBufHex(unsigned char *buf, char *filename, u32 max_bytes)
{
  ASSERT(buf, "buf is NULL");
  ASSERT(filename, "filename is NULL");
  ASSERT(max_bytes > 0, "Trying to read invalid (%lu) number of bytes from %s\n", max_bytes, filename);

  FILE *ifile = fopen(filename, "r");
  ASSERT(ifile, "File '%s' doesn't exist! Check file path\n", filename);
  int wordsRead = 0;
  int totalRead = 0;
  unsigned int word = 0;;
  u32 *wordbuf = (u32*)buf;

  while ((wordsRead = fscanf(ifile, "%x\n", &word)) != -1) {
    wordbuf[totalRead++] = word;
    if (totalRead*sizeof(u32) >= max_bytes) {
      EPRINTF("Read %d words (0x%x bytes), stopping read\n", totalRead, totalRead*sizeof(u32));
      break;
    }
  }
  fclose(ifile);
  return totalRead*sizeof(u32);
}

void bufToFileHex(char *filename, unsigned char *buf, u32 max_bytes)
{
  ASSERT(buf, "buf is NULL");
  ASSERT(filename, "filename is NULL");
  ASSERT(max_bytes > 0, "Trying to write invalid (%lu) number of bytes to %s\n", max_bytes, filename);

  FILE *ofile = fopen(filename, "w");
  int wordsWritten = 0;
  u32 *wordbuf = (u32*)buf;
  ASSERT(ofile, "Could not open file %s for writing\n", filename);

  while ((wordsWritten*sizeof(u32)) != max_bytes) {
    ASSERT(fprintf(ofile, "%.8lx\n", wordbuf[wordsWritten]), "fprintf failed\n");
    wordsWritten++;
  }

  fclose(ofile);
}


void bufToFile(char *filename, unsigned char *buf, u32 max_bytes)
{
  ASSERT(buf, "buf is NULL");
  ASSERT(filename, "filename is NULL");
  ASSERT(max_bytes > 0, "Trying to write invalid (%lu) number of bytes to %s\n", max_bytes, filename);

  FILE *ofile = fopen(filename, "wb");
  int bytesWritten = 0;
  ASSERT(ofile, "Could not open file %s for writing\n", filename);
  bytesWritten = fwrite(buf, sizeof(char), max_bytes, ofile);
  ASSERT(bytesWritten > 0, "Wrote invalid (%d) number of bytes to %s\n", bytesWritten, filename);
  fclose(ofile);
}

void printHelp(int argc, char **argv)
{
  EPRINTF("EE 180 Lab2 driver\n");
  EPRINTF("Usage: %s -p <inst buffer file> OPTS\n", argv[0]);
  EPRINTF("OPTS can be a combination of the following:\n");
  EPRINTF("-p <file> :  Load MIPS instruction buffer with provided textual hex file, one word per line\n");
  EPRINTF("-w        :  Get input video from webcam. Runs program in a loop for every frame\n");
  EPRINTF("-f <file> :  Get input video from file. Runs program in a loop for every frame\n");
  EPRINTF("-i <file> :  Load MIPS data input buffer with provided hex file. Atleast one of -w, -f or -i must be specified\n");
  EPRINTF("-o <file> :  Dump MIPS data output buffer into text file in hex format, one word per line\n");
  EPRINTF("-d        :  Enable Lab 2 driver debug messages\n");
}

void printOpts(struct opts *opts)
{
  EPRINTF("-------- Running driver with the following options ---------\n");
  EPRINTF("Webcam input............ : %d\n", opts->webcam);
  EPRINTF("Video file input........ : %s\n", opts->videoFile);
  EPRINTF("Instruction buffer file..: %s\n", opts->instBuf);
  EPRINTF("Input buffer file........: %s\n", opts->inBuf);
  EPRINTF("Output buffer file ......: %s\n", opts->outBuf);
  EPRINTF("------------------------------------------------------------\n");
}

void parseOpts(int argc, char **argv, struct opts *opts)
{
  int c;
  int inputSrc = 0;
  memset(opts, 0, sizeof(*opts));
  if (argc == 1) {
    printHelp(argc, argv);
    exit(0);
  }
  while ((c = getopt (argc, argv, "wf:p:i:o:d")) != -1) {
    switch (c) {
      case 'w':
        if (inputSrc > 0) {
          printHelp(argc, argv);
          exit(-1);
        }
        else {
          opts->webcam = 1;
          inputSrc = 1;
        }
        break;
      case 'f':
        if (inputSrc > 0) {
          printHelp(argc, argv);
          exit(-1);
        }
        else {
          opts->videoFile = optarg;
          inputSrc = 1;
        }
        break;
      case 'p':
        opts->instBuf = optarg;
        break;
      case 'i':
        if (inputSrc > 0) {
          printHelp(argc, argv);
          exit(-1);
        }
        else {
          opts->inBuf = optarg;
          inputSrc = 1;
        }
        break;
      case 'o':
        opts->outBuf = optarg;
        break;
      case 'd':
        opts->debug = 1;
        break;
      case '?':
        if (optopt == 'f' || optopt == 'p' || optopt == 'i' || optopt == 'o') {
          EPRINTF("Option %c requires an argument\n", optopt);
        }
        else if (isprint(optopt)) {
          EPRINTF("Unknown option %c\n", optopt);
        }
        else {
          EPRINTF("Unknown option character `\\x%x'\n", optopt);
        }
      default:
        printHelp(argc, argv);
        exit(-1);
    }
  }

  // Validate options
  if (!opts->instBuf) {
    printHelp(argc, argv);
    exit(-1);
  }
  else if (inputSrc == 0) {
    EPRINTF("Atleast one of -w, -f or -i must be specified\n");
    printHelp(argc, argv);
    exit(-1);
  }

  if (opts->webcam || opts->videoFile) {
    opts->sobelMode = 1;
  }
  printOpts(opts);
}

double getTime()
{
  double to = 0.0;
  struct timespec ts;
  clock_gettime (CLOCK_MONOTONIC, &ts);
  to = (double) (ts.tv_sec);
  to = (double) (to * 1000 + (double)(ts.tv_nsec) / 1000000) ;
  return to;
}

