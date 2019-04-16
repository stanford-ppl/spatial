/*
 * Adapted from ISL's isl_polyhedron_sample example
 */

#include <assert.h>
#include <isl/vec.h>
#include <isl/set.h>
#include <string.h>

int main(int argc, char **argv)
{
    float version = 1.2;
    if (argc == 2 && strcmp(argv[1], "-version") == 0) {
        fprintf(stdout, "Version: %.1f\n", version);
        return 0;
    }
    else {
    	struct isl_ctx *ctx = isl_ctx_alloc();
    	struct isl_basic_set *bset;

        // Don't buffer stdout (it's buffered in compiler)
    	setbuf(stdout, NULL);
    	//setbuf(stderr, NULL);

        while(1) {
    	    bset = isl_basic_set_read_from_file(ctx, stdin);

            if (isl_basic_set_plain_is_empty(bset)) {
                printf("0");
            }
            else {
                // Is there "really" a collision?
                if (isl_basic_set_is_empty(bset)) {
                    printf("0");
                } else {
                    printf("1");
                }
            }

    	    isl_basic_set_free(bset);
        }

    	isl_ctx_free(ctx);

    	return 0;
    }
}
