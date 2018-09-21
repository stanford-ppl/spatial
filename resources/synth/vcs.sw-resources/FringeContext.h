#ifndef __FRINGE_CONTEXT_H__
#define __FRINGE_CONTEXT_H__

/**
 * Top-level target-specific Fringe Context API
 */

#ifdef SIM
#include "FringeContextSim.h"
typedef FringeContextSim FringeContext;

#elif defined ZYNQ
#include "FringeContextZynq.h"
typedef FringeContextZynq FringeContext;

#elif defined VCS
#include "FringeContextVCS.h"
typedef FringeContextVCS FringeContext;
#endif

#endif
