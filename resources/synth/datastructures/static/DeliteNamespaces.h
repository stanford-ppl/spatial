#ifndef __DELITE_NAMESPACES__
#define __DELITE_NAMESPACES__

//#define __USE_STD_STRING__
#ifdef __USE_STD_STRING__
#include <string>
using std::string;
#else
#include "DeliteString.h"
using delite::string;
#endif

#endif
