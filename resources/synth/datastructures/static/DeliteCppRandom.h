#ifndef __DELITE_CPP_RAND_H__
#define __DELITE_CPP_RAND_H__

#include <math.h>
#include <time.h>

/* ported from Java Random */
class DeliteCppRandom {
    
    private: 
    int64_t seed;
    
    const static int64_t multiplier = 0x5DEECE66DLL;
    const static int64_t addend = 0xBLL;
    const static int64_t mask = (1LL << 48) - 1;
    
    double nextNextGaussian;
    bool haveNextNextGaussian;
	
    public:

    DeliteCppRandom(int32_t threadId) {
        int64_t seedUniquifier = 8682522807148013LL; //TODO: better spread of seeds across threads
        this->seed = initialScramble(seedUniquifier + threadId + (int64_t)clock());
    	haveNextNextGaussian = false;
    }

    DeliteCppRandom(int64_t seed) {
        this->seed = initialScramble(seed);
    	haveNextNextGaussian = false;
    }

    static int64_t initialScramble(int64_t seed) {
        return (seed ^ multiplier) & mask;
    }
    
    int32_t next(int32_t bits) {
        int64_t nextseed;
        int64_t oldseed = this->seed;
        nextseed = (oldseed * multiplier + addend) & mask;
        this->seed = nextseed;
        return (int32_t)(((int64_t) nextseed) >> (48 - bits));
    }

    void setSeed(int64_t seed) {
        this->seed = initialScramble(seed);
        haveNextNextGaussian = false;
    }
	
    int32_t nextInt(int32_t n) {
        if ((n & -n) == n) // i.e., n is a power of 2
            return (int32_t)((n * (int64_t)next(31)) >> 31);
        
        int32_t bits, val;
        do {
            bits = next(31);
            val = bits % n;
        } while (bits - val + (n-1) < 0);
        return val;
    }

    int32_t nextInt() {
        return next(32);
    }
	
    int64_t nextLong() {
        return ((int64_t)(next(32)) << 32) + next(32);
    }
	
    bool nextBoolean() {
        return next(1) != 0;
    }
	
    float nextFloat() {
        return next(24) / ((float)(1 << 24));
    }
	
    double nextDouble() {
        return (((int64_t)(next(26)) << 27) + next(27)) / (double)(1LL << 53);
    }
	
    double nextGaussian() {
	// See Knuth, ACP, Section 3.4.1 Algorithm C.
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double multiplier = sqrt(-2 * log(s)/s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }

};

#endif

