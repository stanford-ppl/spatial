#include <spawn.h>
#include <errno.h>
#include <cstring>
#include <string>
#include <stdio.h>
#include <vector>
#include <queue>
#include <set>
#include <map>
#include <poll.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <unistd.h>
#include <stdlib.h>
#include <iostream>

#include "xsi_loader.h"

using namespace std;

#include "simDefs.h"
#include "channel.h"

#include "vc_hdrs.h"
// #include "svdpi_src.h"

// #include <DRAM.h>
// #include <Streams.h>

// See xsi.h header for more details on how Verilog values are stored as aVal/bVal pairs

// constants 
const s_xsi_vlog_logicval dummy1  = {0X00000000, 0X00000000};
const s_xsi_vlog_logicval three_val  = {0X00000003, 0X00000000};
const s_xsi_vlog_logicval dummy2  = {0X00000000, 0X00000000};
const s_xsi_vlog_logicval eight_val  = {0X00000008, 0X00000000};
const s_xsi_vlog_logicval dummy3  = {0X00000000, 0X00000000};
const s_xsi_vlog_logicval two_val  = {0X00000002, 0X00000000};
const s_xsi_vlog_logicval dummy4  = {0X00000000, 0X00000000};
const s_xsi_vlog_logicval one_val  = {0X00000001, 0X00000000};
const s_xsi_vlog_logicval dummy5  = {0X00000000, 0X00000000};
const s_xsi_vlog_logicval zero_val = {0X00000000, 0X00000000};

void append_logic_val_bit_to_string(std::string& retVal, int aVal, int bVal)
{
     if(aVal == 0) {
        if(bVal == 0) {
           retVal +="0";
        } else {
           retVal +="Z";
        }
     } else { // aVal == 1
        if(bVal == 0) {
           retVal +="1";
        } else {
           retVal +="X";
        }
     }
}



void append_logic_val_to_string(std::string& retVal, int aVal, int bVal, int max_bits)
{
   int bit_mask = 0X00000001;
   int aVal_bit, bVal_bit;
   for(int k=max_bits; k>=0; k--) {
      aVal_bit = (aVal >> k ) & bit_mask;
      bVal_bit = (bVal >> k ) & bit_mask;
      append_logic_val_bit_to_string(retVal, aVal_bit, bVal_bit);
   }
}

std::string logic_val_to_string(s_xsi_vlog_logicval* value, int size)
{
   std::string retVal;

   int num_words = size/32 + 1;
   int max_lastword_bit = size %32 - 1;

   // last word may have unfilled bits
   int  aVal = value[num_words -1].aVal;
   int  bVal = value[num_words -1].bVal;
   append_logic_val_to_string(retVal, aVal, bVal, max_lastword_bit);
   
   // this is for fully filled 32 bit aVal/bVal structs
   for(int k = num_words - 2; k>=0; k--) {
      aVal = value[k].aVal;
      bVal = value[k].bVal;
      append_logic_val_to_string(retVal, aVal, bVal, 31);
   }
   return retVal;
}

std::string getcurrentdir()
{
#if defined(_WIN32)
    char buf[MAX_PATH];
    GetCurrentDirectory(sizeof(buf), buf);
    buf[sizeof(buf)-1] = 0;
    return buf;
#else
    char buf[1024];
    getcwd(buf, sizeof(buf)-1);
    buf[sizeof(buf)-1] = 0;
    return buf;
#endif
}

void step(Xsi::Loader& Xsi_Instance, int clk, int steps)
{
  for (int i = 0; i < steps; i++) {
    Xsi_Instance.put_value(clk, &one_val);
    Xsi_Instance.run(10);
    Xsi_Instance.put_value(clk, &zero_val);
    Xsi_Instance.run(10);
  }
}
std::string get_done(Xsi::Loader& Xsi_Instance, int clk, int rdata, int raddr)
{
  s_xsi_vlog_logicval valraw;
  Xsi_Instance.put_value(raddr, &one_val);
  step(Xsi_Instance, clk, 1);
  Xsi_Instance.get_value(rdata, &valraw);

  std::string val = logic_val_to_string(&valraw, 8);
  std::cout << val << std::endl;
  std::string ret(1, val.back());
  return ret;
}

int main(int argc, char **argv)
{
    std::string cwd = getcurrentdir();
    std::string simengine_libname = "librdi_simulator_kernel";

#if defined(_WIN32)
    const char* lib_extension = ".dll";
#else
    const char* lib_extension = ".so";
#endif
    simengine_libname += lib_extension;

    std::string design_libname = getcurrentdir() + "/xsim.dir/accel/xsimk" + lib_extension;

    std::cout << "Design DLL     : " << design_libname << std::endl;
    std::cout << "Sim Engine DLL : " << simengine_libname << std::endl;

    // Output value (Up to 32 bit requires just one struct
    s_xsi_vlog_logicval count_val = {0X00000000, 0X00000000};

    // Ports
    int reset;
    int clk;
    int enable;
    int count;
    int wdata;
    int waddr;
    int wen;
    int rdata;
    int raddr;

    // my variables 
    int count_success = 0;
    int status = 0;


    try {
        Xsi::Loader Xsi_Instance(design_libname, simengine_libname);
        s_xsi_setup_info info;
        memset(&info, 0, sizeof(info));
        info.logFileName = NULL;
        char wdbName[] = "test.wdb";
        info.wdbFileName = wdbName;
        Xsi_Instance.open(&info);
        Xsi_Instance.trace_all();
        reset = Xsi_Instance.get_port_number("reset");
        if(reset <0) {
          std::cerr << "ERROR: reset not found" << std::endl;
          exit(1);
        }
        clk = Xsi_Instance.get_port_number("clock");
        if(clk <0) {
          std::cerr << "ERROR: clk not found" << std::endl;
          exit(1);
        }
        waddr = Xsi_Instance.get_port_number("io_waddr");
        if(waddr <0) {
          std::cerr << "ERROR: enable not found" << std::endl;
          exit(1);
        }
        wdata = Xsi_Instance.get_port_number("io_wdata");
        if(wdata <0) {
          std::cerr << "ERROR: count not found" << std::endl;
          exit(1);
        }
        wen = Xsi_Instance.get_port_number("io_wen");
        if(wen <0) {
          std::cerr << "ERROR: count not found" << std::endl;
          exit(1);
        }
        raddr = Xsi_Instance.get_port_number("io_raddr");
        if(raddr <0) {
          std::cerr << "ERROR: enable not found" << std::endl;
          exit(1);
        }
        rdata = Xsi_Instance.get_port_number("io_rdata");
        if(rdata <0) {
          std::cerr << "ERROR: count not found" << std::endl;
          exit(1);
        }

        Xsi_Instance.put_value(wen, &zero_val);
        step(Xsi_Instance, clk, 1);

        Xsi_Instance.put_value(reset, &one_val);
        step(Xsi_Instance, clk, 5);

        Xsi_Instance.put_value(reset, &zero_val);
        step(Xsi_Instance, clk, 1);

        // Arg 2
        Xsi_Instance.put_value(waddr, &two_val);
        Xsi_Instance.put_value(wdata, &eight_val);
        Xsi_Instance.put_value(wen, &one_val);
        step(Xsi_Instance, clk, 1);

        Xsi_Instance.put_value(wen, &zero_val);
        step(Xsi_Instance, clk, 1);

        // Arg 1
        Xsi_Instance.put_value(waddr, &one_val);
        Xsi_Instance.put_value(wdata, &zero_val);
        Xsi_Instance.put_value(wen, &one_val);
        step(Xsi_Instance, clk, 1);

        Xsi_Instance.put_value(wen, &zero_val);
        step(Xsi_Instance, clk, 1);

        // Arg 0
        Xsi_Instance.put_value(waddr, &zero_val);
        Xsi_Instance.put_value(wdata, &one_val);
        Xsi_Instance.put_value(wen, &one_val);
        step(Xsi_Instance, clk, 1);

        Xsi_Instance.put_value(wen, &zero_val);


        bool done = false;
        int cycles = 0;
        int TIMEOUT = 500;
        while (!done) {
          cycles++;
          step(Xsi_Instance, clk, 10);
          done = get_done(Xsi_Instance, clk, rdata, raddr) == "1";
          std::cout << cycles << ": " << std::endl;
          if (cycles > TIMEOUT) done = true;
        }

        if (cycles > TIMEOUT) {
          std::cout << "ERROR: Timeout (" << cycles << " cycles)" << std::endl;
        }
        std::cout << "Ran for " << cycles << " cycles." << std::endl;

        Xsi_Instance.put_value(raddr, &three_val);
        step(Xsi_Instance, clk, 3);
        Xsi_Instance.get_value(rdata, &count_val);
        step(Xsi_Instance, clk, 3);

        std::string count_val_string = logic_val_to_string(&count_val, 8);
        std::cout << rdata << "," << count_val_string << std::endl;

        
        // std::string count_val_string;
        // // The reset is done. Now start counting
        // std::cout << "\n *** starting to count ***\n";
        // for (int i=0; i < 15; i++) {
        //    Xsi_Instance.put_value(clk, &one_val);
        //    Xsi_Instance.run(10);

        //    // read the output
        //    Xsi_Instance.get_value(count, &count_val);
        
        //    count_val_string = logic_val_to_string(&count_val, 4);
        //    std::cout << count_val_string << std::endl;
        //    // if( count_val_string.compare(expected_out[i]) == 0) {
        //    //    count_success++;
        //    // }
        //    // Put clk to zero
        //    Xsi_Instance.put_value(clk, &zero_val);
        //    Xsi_Instance.run(10);
        // }
        std::cout << "\n *** done counting ***\n";
        
        std::cout << "Total successful checks: " << count_success <<"\n";
        status = (count_success == 15) ? 0:1;

        // Just a check to rewind time to 0
        Xsi_Instance.restart();

    }
    catch (std::exception& e) {
        std::cerr << "ERROR: An exception occurred: " << e.what() << std::endl;
        status = 2;
    }
    catch (...) {
        std::cerr << "ERROR: An unknown exception occurred." << std::endl;
        status = 3;
    }



}




// extern char **environ;

// // Slave channels from HOST
// Channel *cmdChannel = NULL;
// Channel *respChannel = NULL;

// int sendResp(simCmd *cmd) {
//   simCmd resp;
//   resp.id = cmd->id;
//   resp.cmd = cmd->cmd;
//   resp.size = cmd->size;
//   switch (cmd->cmd) {
//     case READY:
//       resp.size = 0;
//       break;
//     default:
//       EPRINTF("[SIM] Command %d not supported!\n", cmd->cmd);
//       exit(-1);
//   }

//   respChannel->send(&resp);
//   return cmd->id;
// }

// typedef struct {
//   simCmd *cmd;
//   int waitCycles;
// } pendingOp;
// // Set containing allocated pages
// set<uint64_t> allocatedPages;
// queue<pendingOp*> pendingOps;
// uint64_t numCycles = 0;

// extern "C" {
//   // Callback function from SV when there is valid data
//   // Currently output stream is always ready, so there is no feedback going from C++ -> SV
//   void readOutputStream(int data, int tag, int last) {
//     // view addr as uint64_t without doing sign extension
//     uint32_t udata = *(uint32_t*)&data;
//     uint32_t utag = *(uint32_t*)&tag;
//     bool blast = last > 0;

//     // Currently just print read data out to console
//     outStream->recv(udata, utag, blast);
//   }
// }

// extern "C" {
//   // Function is called every clock cycle
//   int tick() {
//     bool exitTick = false;
//     int finishSim = 0;
//     getCycles((long long int*)(&numCycles));

//     // Handle pending operations, if any
//     if (pendingOps.size() > 0) {
// //      simCmd *cmd = pendingOps.front();
//       pendingOp *op = pendingOps.front();
//       op->waitCycles--;
//       if (op->waitCycles == 0) {
//         pendingOps.pop();
//         simCmd *cmd = op->cmd;
//         switch (cmd->cmd) {
//           case READ_REG:
//             // Construct and send response
//             simCmd resp;
//             resp.id = cmd->id;
//             resp.cmd = cmd->cmd;
//             SV_BIT_PACKED_ARRAY(32, rdataHi);
//             SV_BIT_PACKED_ARRAY(32, rdataLo);
//             readRegRdataHi32((svBitVec32*)&rdataHi);
//             readRegRdataLo32((svBitVec32*)&rdataLo);
//             *(uint32_t*)resp.data = (uint32_t)*rdataLo;
//             *((uint32_t*)resp.data + 1) = (uint32_t)*rdataHi;
//             resp.size = sizeof(uint64_t);
//             respChannel->send(&resp);
//             break;
//           default:
//             EPRINTF("[SIM] Ignoring unknown pending command %u\n", cmd->cmd);
//             break;
//         }
//         free(op);
//         free(cmd);
//       } else {
//         exitTick = true;
//       }
//     }

//     // Drain an element from DRAM queue if it exists
//     checkAndSendDRAMResponse();

//     // Check if input stream has new data
//     inStream->send();

//     // Handle new incoming operations
//     while (!exitTick) {
//       simCmd *cmd = (simCmd*) cmdChannel->recv();
//       simCmd readResp;
//       uint32_t reg = 0;
//       uint64_t data = 0;
//       switch (cmd->cmd) {
//         case MALLOC: {
//           size_t size = *(size_t*)cmd->data;
//           int fd = open("/dev/zero", O_RDWR);
//           void *ptr = mmap(0, size, PROT_READ|PROT_WRITE, MAP_PRIVATE, fd, 0);
//           close(fd);

//           simCmd resp;
//           resp.id = cmd->id;
//           resp.cmd = cmd->cmd;
//           *(uint64_t*)resp.data = (uint64_t)ptr;
//           resp.size = sizeof(size_t);
//           EPRINTF("[SIM] MALLOC(%lu), returning %p - %p\n", size, (void*)ptr, (void*)((uint8_t*)ptr + size));
//           respChannel->send(&resp);

//           break;
//         }
//         case FREE: {
//           void *ptr = (void*)(*(uint64_t*)cmd->data);
//           ASSERT(ptr != NULL, "Attempting to call free on null pointer\n");
//           EPRINTF("[SIM] FREE(%p)\n", ptr);
//           break;
//         }
//         case MEMCPY_H2D: {
//           uint64_t *data = (uint64_t*)cmd->data;
//           void *dst = (void*)data[0];
//           size_t size = data[1];

//           EPRINTF("[SIM] Received memcpy request to %p, size %lu\n", (void*)dst, size);

//           // Now to receive 'size' bytes from the cmd stream
//           cmdChannel->recvFixedBytes(dst, size);

//           // Send ack back indicating end of memcpy
//           simCmd resp;
//           resp.id = cmd->id;
//           resp.cmd = cmd->cmd;
//           resp.size = 0;
//           respChannel->send(&resp);
//           break;
//         }
//         case MEMCPY_D2H: {
//           // Transfer 'size' bytes from src
//           uint64_t *data = (uint64_t*)cmd->data;
//           void *src = (void*)data[0];
//           size_t size = data[1];

//           // Now to receive 'size' bytes from the cmd stream
//           respChannel->sendFixedBytes(src, size);
//           break;
//         }
//         case RESET:
//           rst();
//           exitTick = true;
//           break;
//         case START:
//           start();
//           exitTick = true;
//           break;
//         case STEP: {
//           exitTick = true;
//           if (!useIdealDRAM) {
//             mem->update();
//           }
//           break;
//         }
//         case GET_CYCLES: {
//           exitTick = true;
//           simCmd resp;
//           resp.id = cmd->id;
//           resp.cmd = cmd->cmd;
//           *(uint64_t*)resp.data = numCycles;
//           resp.size = sizeof(size_t);
//           respChannel->send(&resp);
//           break;
//         }
//         case READ_REG: {
//             reg = *((uint32_t*)cmd->data);

//             // Issue read addr
//             readRegRaddr(reg);

//             // Append to pending ops - will return in the next cycle
//             simCmd *pendingCmd = (simCmd*) malloc(sizeof(simCmd));
//             memcpy(pendingCmd, cmd, sizeof(simCmd));
//             pendingOp *op = (pendingOp*)malloc(sizeof(pendingOp));
//             op->cmd = pendingCmd;
//             op->waitCycles = 2;
//             pendingOps.push(op);

//             exitTick = true;
//             break;
//          }
//         case WRITE_REG: {
//             reg = *((uint32_t*)cmd->data);
//             data = *((uint64_t*)((uint32_t*)cmd->data + 1));

//             // Perform write
//             writeReg(reg, data);
//             exitTick = true;
//             break;
//           }
//         case FIN:
//           if (!useIdealDRAM) {
//             mem->printStats(true);
//           }
//           finishSim = 1;

//           simCmd resp;
//           resp.id = cmd->id;
//           resp.cmd = cmd->cmd;
//           resp.size = 0;
//           EPRINTF("[SIM] FIN received, terminating\n");
//           respChannel->send(&resp);

//           exitTick = true;
//           break;
//         default:
//           break;
//       }
//     }
//     return finishSim;
//   }

//   void printAllEnv() {
//     EPRINTF("[SIM]  *** All environment variables visible *** \n");
//     int tmp = 0;
//     while (environ[tmp]) {
//       EPRINTF("[SIM] environ[%d] = %s\n", tmp, environ[tmp]);
//       tmp++;
//     }
//     EPRINTF("[SIM]  ***************************************** \n");
//   }

//   // Called before simulation begins
//   void sim_init() {
//     EPRINTF("[SIM] Sim process started!\n");
//     prctl(PR_SET_PDEATHSIG, SIGHUP);

//     /**
//      * Open slave interface to host {
//      */
//       // 0. Create Channel structures
//       cmdChannel = new Channel(SIM_CMD_FD, -1, sizeof(simCmd));
//       respChannel = new Channel(-1, SIM_RESP_FD, sizeof(simCmd));

//       // 1. Read command
//       simCmd *cmd = (simCmd*) cmdChannel->recv();

//       // 2. Send response
//       sendResp(cmd);
//     /**} End Slave interface to Host */

//     /**
//      * Set VPD / VCD based on environment variables
//      */
//     char *vpd = getenv("VPD_ON");
//     if (vpd != NULL) {
//       if (vpd[0] != 0 && atoi(vpd) > 0) {
//         startVPD();
//         EPRINTF("[SIM] VPD Waveforms ENABLED\n");
//       } else {
//         EPRINTF("[SIM] VPD Waveforms DISABLED\n");
//       }
//     } else {
//       EPRINTF("[SIM] VPD Waveforms DISABLED\n");
//     }

//     char *vcd = getenv("VCD_ON");
//     if (vcd != NULL) {
//       if (vcd[0] != 0 && atoi(vcd) > 0) {
//         startVCD();
//         EPRINTF("[SIM] VCD Waveforms ENABLED\n");
//       } else {
//         EPRINTF("[SIM] VCD Waveforms DISABLED\n");
//       }
//     } else {
//       EPRINTF("[SIM] VCD Waveforms DISABLED\n");
//     }



//     /**
//      * Initialize peripheral simulators
//      */
//     initDRAM();
//     initStreams();


//   }
// }
