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

#elif defined KCU1500
#include "FringeContextKCU1500.h"
typedef FringeContextKCU1500 FringeContext;
#endif

#endif
