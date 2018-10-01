#ifndef __FRINGE_CONTEXT_BASE_H__
#define __FRINGE_CONTEXT_BASE_H__

template <class T>
class FringeContextBase {
public:
  T *dut = NULL;
  std::string path = "";

  FringeContextBase(std::string p) {
    path = p;
  }
  virtual void load() = 0;
  virtual uint64_t malloc(size_t bytes) = 0;
  virtual void free(uint64_t buf) = 0;
  virtual void memcpy(uint64_t devmem, void* hostmem, size_t size) = 0;
  virtual void memcpy(void* hostmem, uint64_t devmem, size_t size) = 0;
  virtual void run() = 0;
  virtual void writeReg(uint32_t reg, uint64_t data) = 0;
  virtual uint64_t readReg(uint32_t reg) = 0;
  virtual uint64_t getArg(uint32_t arg, bool isIO) = 0;
  virtual uint64_t getArg64(uint32_t arg, bool isIO) = 0;
  virtual void setArg(uint32_t arg, uint64_t data, bool isIO) = 0;
  virtual void setNumArgIns(uint32_t number) = 0;
  virtual void setNumArgIOs(uint32_t number) = 0;
  virtual void setNumArgOutInstrs(uint32_t number) = 0;
  virtual void setNumArgOuts(uint32_t number) = 0;
  virtual void setNumEarlyExits(uint32_t number) = 0;
  virtual void flushCache(uint32_t kb) = 0;

  ~FringeContextBase() {
    delete dut;
  }
};

// Fringe error codes


// Fringe APIs - implemented only for simulation
void fringeInit(int argc, char **argv);

#endif
