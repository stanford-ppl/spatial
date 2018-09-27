#include "DeliteStandaloneMain.h"

extern void Application(cppDeliteArray< string > *x0);

int main(int argc, char *argv[]) {

  printf("** Starting Standalone C++ Execution **\n"); 

  // set the number of threads for parallel execution
  omp_set_num_threads(DELITE_CPP);

  // create x0 symbol for user inputs
  cppDeliteArray< string > *x0 = new cppDeliteArray< string >(argc-1);
  for(int i=0; i<argc-1; i++) {
    x0->data[i] = string(argv[i+1]);
  }

  // Start the generated application
  Application(x0);

  printf("** Finished Standalone C++ Execution **\n"); 

  return 0;
}
