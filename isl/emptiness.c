/*
 * Adapted from ISL's isl_polyhedron_sample example
 */

#include <assert.h>
#include <isl/vec.h>
#include <isl/set.h>

int main(int argc, char **argv)
{
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
            printf("1");
        }

	    isl_basic_set_free(bset);
    }

	isl_ctx_free(ctx);

	return 0;
}
