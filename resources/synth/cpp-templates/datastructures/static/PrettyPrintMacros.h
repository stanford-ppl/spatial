#define KNRM  "\x1B[0m"
#define KRED  "\x1B[31m"
#define KGRN  "\x1B[32m"
#define KYEL  "\x1B[33m"
#define KBLU  "\x1B[34m"
#define KMAG  "\x1B[35m"
#define KCYN  "\x1B[36m"
#define KWHT  "\x1B[37m"

#define PRINT_RED(...) \
	printf("%s", KRED);  \
  printf(__VA_ARGS__); \
	printf("%s", KNRM);  \

#define PRINT_GREEN(...) \
	printf("%s", KGRN);  \
  printf(__VA_ARGS__); \
	printf("%s", KNRM);  \
