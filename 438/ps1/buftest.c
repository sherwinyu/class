/*
 * SIMPLE Buffer Tester
 *
*/

#include "postgres.h"
#include "storage/buf_internals.h"
#include "storage/bufmgr.h"

int NBuffers = 4;
#undef INIT_BUFFERTAG
#define INIT_BUFFERTAG(a,r,b) \
( \
	(a).rnode.spcNode = (a).rnode.dbNode = 0, \
	(a).rnode.relNode = (r), \
	(a).blockNum = (b) \
)

void
printBuffers() {
	int i;
	BufferDesc *buf;
	char *block;

	buf = BufferDescriptors;

	for (i=0; i<NBuffers; block+=BLCKSZ, buf++, i++) {
		if (buf->refcount == 0)
			printf("[%1s]",((char *) MAKE_PTR(buf->data)));
		else
			printf("(%1s)",((char *) MAKE_PTR(buf->data)));

	}
       //  printf("\t");
        // for (i=0; i<NBuffers; i++)
          // printf("%d", refbits[i]);
	printf("\n");
}

BufferDesc*
findPage(BufferTag *btag, int *cdb_found_index) {
        BufferDesc *buf;


	buf = StrategyBufferLookup(btag, false, cdb_found_index);
        return buf;
}

void
writePage(char pg, BufferDesc *buf) {
        *((char *) MAKE_PTR(buf->data)) = pg;
}

BufferDesc *
getPage(Oid rel, int bno) {
	BufferTag  btag;
        BufferDesc *buf;
        int cdb_replace_index;
        int cdb_found_index;

	INIT_BUFFERTAG(btag, rel, bno);

        // Find page if in memory
        buf = findPage(&btag, &cdb_found_index);
        if (buf == NULL) {
                // Get a free page
                buf = StrategyGetBuffer(&cdb_replace_index);

		if (buf == NULL) {
			printf("Out of buffers!\n");
			exit(0);
		}
		else if (buf->refcount > 0) {
			printf("Pinned buffer!\n");
			exit(0);
		}

		buf->refcount = 1;
		PrivateRefCount[BufferDescriptorGetBuffer(buf) - 1] = 1;
        } else {
		buf->refcount++;
		PrivateRefCount[BufferDescriptorGetBuffer(buf) - 1]++;
		return buf;
        }
	StrategyReplaceBuffer(buf, &btag, cdb_found_index, cdb_replace_index);
	buf->tag = btag;
        return buf;
}

void
unpinPage(Oid rel, int bno) {
	BufferTag  btag;
	int        cdb_found_index;
	BufferDesc *buf;

	INIT_BUFFERTAG(btag, rel, bno);
	buf = findPage(&btag, &cdb_found_index);

        if (buf == NULL)
		printf("Page not in memory!\n");
        else {
		buf->refcount--;
		PrivateRefCount[BufferDescriptorGetBuffer(buf) - 1]--;
        }
}

#define GET_WRITE(r, b, v) GET_WRITE_PIN(r, b, v, false)

#define GET_WRITE_PIN(r, b, v, p) \
do { \
	BufferDesc *bdesc; \
	bdesc = getPage((r), (b)); \
	writePage((v), bdesc); \
	if (!(p)) unpinPage((r), (b)); \
} while (0)

int
main(int argc, char *argv[]) {
	printf("Initializing buffer pool with %d pages\n", NBuffers);
	InitBufferPool();
	printf("Beginning tests...\n");
	printBuffers();
	GET_WRITE(0, 0, 'a');
	printBuffers();
	GET_WRITE(0, 1, 'b');
	printBuffers();
	GET_WRITE(0, 2, 'c');
	printBuffers();
	GET_WRITE_PIN(0, 3, 'd', true);
	printBuffers();
	GET_WRITE(0, 4, 'e');
	printBuffers();
	GET_WRITE(0, 5, 'f');
	printBuffers();
	GET_WRITE(0, 6, 'g');
	printBuffers();
	GET_WRITE(0, 4, 'h');
	printBuffers();
	unpinPage(0, 3);
	GET_WRITE(0, 9, 'i');
	printBuffers();
	GET_WRITE(0, 10, 'j');
	printBuffers();
	GET_WRITE(0, 11, 'k');
	printBuffers();
	GET_WRITE(0, 11, 'l');
	printBuffers();
	GET_WRITE(0, 11, 'm');
	printBuffers();
	GET_WRITE(0, 11, 'n');
	printBuffers();
	GET_WRITE(0, 11, 'o');
	printBuffers();
	GET_WRITE(0, 4, 'h');
	printBuffers();
	GET_WRITE(0, 6, 'p');
	printBuffers();
	printf("Finished.\n");
	return 0;
}
