#ifndef PCM_HELPER_H
#define PCM_HELPER_H

#ifdef DELITE_ENABLE_PCM
#include "cpucounters.h"
#endif

struct PCMStats {
	double l2CacheHitRatio;
	double l3CacheHitRatio;
	unsigned int l2Misses;
	unsigned int l3Misses;

	PCMStats() :
		l2CacheHitRatio(0.0),
		l3CacheHitRatio(0.0),
		l2Misses(0),
		l3Misses(0)
	{ }
};

void pcmInit();
void pcmCleanup();
void printPCMStats(PCMStats* stats);

#ifdef DELITE_ENABLE_PCM
PCMStats* getPCMStats(CoreCounterState& before, CoreCounterState& after);
CoreCounterState getCoreCounterState(int32_t tid);
#endif

#endif
