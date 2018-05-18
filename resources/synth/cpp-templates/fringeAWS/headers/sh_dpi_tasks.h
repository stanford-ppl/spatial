// Amazon FPGA Hardware Development Kit
//
// Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Amazon Software License (the "License"). You may not use
// this file except in compliance with the License. A copy of the License is
// located at
//
//    http://aws.amazon.com/asl/
//
// or in the "license" file accompanying this file. This file is distributed on
// an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or
// implied. See the License for the specific language governing permissions and
// limitations under the License.

#ifndef SH_DPI_TASKS
#define SH_DPI_TASKS

#include <stdarg.h>

extern "C" void sv_printf(char *msg);
extern void sv_map_host_memory(uint8_t *memory);

extern "C" void cl_peek(uint64_t addr, uint32_t *data);
extern "C" void cl_poke(uint64_t addr, uint32_t  data);

extern "C" void TMP_que_buffer_to_cl      (uint64_t src_addr, uint64_t cl_addr, uint32_t len);
extern "C" void TMP_start_que_to_cl       ();
extern "C" void TMP_is_dma_to_cl_done     (uint32_t *is_done);

extern "C" void TMP_start_que_to_buffer   ();
extern "C" void TMP_que_cl_to_buffer      (uint64_t dst_addr, uint64_t cl_addr, uint32_t len);
extern "C" void TMP_is_dma_to_buffer_done (uint32_t *is_done);

extern void sv_int_ack(uint32_t int_num);
extern "C" void sv_pause(uint32_t x);

extern "C" void test_main(uint32_t *exit_code);

extern "C" void host_memory_putc(uint64_t addr, uint8_t data)
{
  *(uint8_t *)addr = data;
}

//void host_memory_getc(uint64_t addr, uint8_t *data)
extern "C" uint8_t host_memory_getc(uint64_t addr)
{
  return *(uint8_t *)addr;
}

extern "C" void log_printf(const char *format, ...)
{
  static char sv_msg_buffer[256];
  va_list args;

  va_start(args, format);
  vsprintf(sv_msg_buffer, format, args);
  sv_printf(sv_msg_buffer);

  va_end(args);
}

extern "C" char * convert_to_string_int32_t(int32_t i, char *buf)
{
  sprintf(buf, "%d", i);
  return buf;
}

void int_handler(uint32_t int_num)
{
// Vivado does not support svGetScopeFromName
#ifndef VIVADO_SIM
  svScope scope;
  scope = svGetScopeFromName("tb");
  svSetScope(scope);
#endif

  log_printf("Received interrupt %2d", int_num);
  sv_int_ack(int_num);
}

#define LOW_32b(a)  ((uint32_t)((uint64_t)(a) & 0xffffffff))
#define HIGH_32b(a) ((uint32_t)(((uint64_t)(a)) >> 32L))

#endif
