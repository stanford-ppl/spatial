#ifndef __CHANNEL_H
#define __CHANNEL_H

#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>
#include "commonDefs.h"

#define READ 0
#define WRITE 1

class Channel {
  int pipeFd[2];
  uint8_t *buf;
  int bufSize = -1;
public:
  Channel(int bufSize) {
    if (pipe(pipeFd)) {
      EPRINTF("Failed to create pipe, error = %s\n", strerror(errno));
      exit(-1);
    }

    buf = (uint8_t*) malloc(bufSize);
    memset(buf, 0, bufSize);
    this->bufSize = bufSize;
  }

  Channel(int readfd, int writefd, int bufSize) {
    pipeFd[READ] = readfd;
    pipeFd[WRITE] = writefd;

    buf = (uint8_t*) malloc(bufSize);
    memset(buf, 0, bufSize);
    this->bufSize = bufSize;
  }

  int writeFd() {
    return pipeFd[WRITE];
  }

  int readFd() {
    return pipeFd[READ];
  }

  void send(void *cmd) {
    int bytes = write(pipeFd[WRITE], cmd, bufSize);
    if (bytes < 0) {
      EPRINTF("Error sending cmd, error = %s\n", strerror(errno));
      exit(-1);
    }
  }

  void sendFixedBytes(void *src, size_t numBytes) {
    ASSERT(src, "[sendFixedBytes] Src memory is null\n");
    uint8_t *bsrc = (uint8_t*)src;
    std::vector<pollfd> plist = { {pipeFd[WRITE], POLLOUT} };
    size_t totalBytesWritten = 0;

    for (int rval; (rval=poll(&plist[0], plist.size(), /*timeout*/-1)) > 0; ) {
      if (plist[0].revents & POLLOUT) {
        int bytesWritten = write(pipeFd[WRITE], &bsrc[totalBytesWritten], numBytes-totalBytesWritten);
        if (bytesWritten < 0) {
          EPRINTF("send error: %s\n", strerror(errno));
        } else {
          totalBytesWritten += bytesWritten;
          EPRINTF("Total bytes written: %lu\n", totalBytesWritten);
        }

        if (totalBytesWritten >= numBytes) {
          break;
        }
      } else {
        break; // nothing left to read
      }
    }
  }

	void* recv() {
    memset(buf, 0, bufSize);

    std::vector<pollfd> plist = { {pipeFd[READ], POLLIN} };

    for (int rval; (rval=poll(&plist[0], plist.size(), /*timeout*/-1)) > 0; ) {
      if (plist[0].revents & POLLIN) {
        int bytesread = read(pipeFd[READ], buf, bufSize);
        if (bytesread > 0) {
          break;
        }
      } else {
        break; // nothing left to read
      }
    }
    return (void*)buf;
	}

  void recvFixedBytes(void *dst, size_t numBytes) {
    ASSERT(dst, "[recvFixedBytes] Destination memory is null\n");
    uint8_t *bdst = (uint8_t*)dst;
    std::vector<pollfd> plist = { {pipeFd[READ], POLLIN} };
    size_t totalBytesRead = 0;

    for (int rval; (rval=poll(&plist[0], plist.size(), /*timeout*/-1)) > 0; ) {
      if (plist[0].revents & POLLIN) {
        int bytesRead = read(pipeFd[READ], &bdst[totalBytesRead], numBytes-totalBytesRead);
        if (bytesRead < 0) {
          EPRINTF("recvFixedBytes error @totalBytesRead = %lu, &bdst = %p: %s\n", totalBytesRead, (void*)&bdst[totalBytesRead], strerror(errno));
        } else {
          totalBytesRead += bytesRead;
          EPRINTF("Total bytes read: %lu\n", totalBytesRead);
        }
        if (totalBytesRead >= numBytes) {
          break;
        }
      } else {
        break; // nothing left to read
      }
    }
  }
};

#endif // __CHANNEL_H
