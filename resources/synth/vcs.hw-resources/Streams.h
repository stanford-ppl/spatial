#include <unistd.h>
#include <errno.h>
#include <cstring>
#include <string>
#include <cstdlib>
#include <stdio.h>
#include <vector>
#include <queue>
#include <map>
#include <poll.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/prctl.h>
using namespace std;

#include "vc_hdrs.h"
#include "svdpi_src.h"

class InputStream {
public:
  string filename;
  int fd;

  uint32_t next;
  bool lastEncountered;

  InputStream(string filename) : filename(filename) {
    fd = open(filename.c_str(), O_RDONLY);
    ASSERT(fd != -1, "[InputStream] Error opening file '%s': %s\n", filename.c_str(), strerror(errno));
    next = 0;
    lastEncountered = false;
  }

  void updateNext() {

  }

  // Poll fd for new bytes
  // - New data exists: Read and pass to writeStream
  // - No new data: Return
  void send() {
    uint32_t data = 0;
    static uint32_t tag = 0;
    bool last = false;

    int bytesread = read(fd, &data, sizeof(uint32_t));
    ASSERT(bytesread >= 0, "[InputStream] Error reading file '%s': %s\n", filename.c_str(), strerror(errno));
    if (bytesread < sizeof(uint32_t)) {
      if (lastEncountered) { // If we have already seen last, don't send it again
        last = false;
      } else {
        last = true;
        lastEncountered = true;
      }
    } else {
      last = false;
    }

    writeStream(data, tag++, last);
  }

  ~InputStream() {
    close(fd);
  }
};


class OutputStream {
public:
  string filename;
  int fd;

  OutputStream(string filename) : filename(filename) {
    fd = open(filename.c_str(), O_RDWR | O_CREAT, S_IRUSR | S_IWUSR);
    ASSERT(fd != -1, "[OutputStream] Error opening file '%s': %s\n", filename.c_str(), strerror(errno));
  }

  // Callback function called from SV -> sim.cpp
  // Just write data into a file
  // [TODO] In current scheme, output stream is always ready. Model backpressure more accurately.
  void recv(uint32_t udata, uint32_t utag, bool blast) {
    // Currently print read data out to console in addition to file
    //EPRINTF("[readOutputStream] data = %08x, tag = %08x, last = %x\n", udata, utag, blast);

    int bytes = write(fd, &udata, sizeof(uint32_t));   // Ignore tag for now
    ASSERT(bytes >= 0, "[OutputStream] Error writing to file '%s': %s\n", filename.c_str(), strerror(errno));
  }

  ~OutputStream() {
    close(fd);
  }
};

// Input stream
InputStream *inStream = NULL;

// Output stream
OutputStream *outStream = NULL;

void initStreams() {
  // Initialize simulation streams
  inStream = new InputStream("in.txt");
  outStream = new OutputStream("out.txt");
}

