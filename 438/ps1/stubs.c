// Version 2, 1/27/02

#include "postgres.h"
#include "storage/buf_internals.h"

extern int NBuffers;
bool assert_enabled;

// ****************************** 
// src/backend/utils/error/elog.c
// ******************************
ErrorContextCallback *error_context_stack = NULL;

void 
elog_start(const char *filename, int lineno, const char *funcname) {

}

void
elog_finish(int elevel, const char *fmt,...)
{
	fprintf(stderr, "ERROR: %s\n", fmt);
	exit(1);
}

// ******************************
// src/backend/storage/lmgr/lwlock.c
// ******************************

LWLockId LWLockAssign(void) {
	return 0;
}

void
ProcSendSignal(BackendId procId) {
} 

void *MemoryContextAlloc(MemoryContext context, Size size) {
	return malloc(size);
}

void pfree(void *pointer) {
	free(pointer);
}

void *
ShmemInitStruct(const char *name, Size size, bool *foundPtr)
{
	*foundPtr = false;
	return malloc(size);
}

// ******************************
// src/backend/storage/ipc/shmem.c
// ******************************
   
SHMEM_OFFSET ShmemBase;

// ****************************** 
// src/backend/storage/buffer/buf_init.c
// ******************************

long 	*PrivateRefCount;

int	Data_Descriptors;
int	Free_List_Descriptor;
int	Lookup_List_Descriptor;
int	Num_Descriptors;
BufferDesc *BufferDescriptors;

void
InitBufferPool (int size) {
	char	*BufferBlocks;
	int	i;

	PrivateRefCount = (long *) malloc(NBuffers * sizeof(long));

	Data_Descriptors = NBuffers;
	Free_List_Descriptor = Data_Descriptors;
	Lookup_List_Descriptor = Data_Descriptors + 1;
	Num_Descriptors = Data_Descriptors + 1;

	// Altered to use regular memory
	BufferDescriptors = (BufferDesc *) malloc (Num_Descriptors * sizeof(BufferDesc));
        BufferBlocks = (char *) malloc (NBuffers * BLCKSZ);

	// Setup SHMEM_BASE
	ShmemBase = (unsigned long) BufferBlocks;

	if (false)
	{
	}
	else
	{
		BufferDesc *buf;
		char       *block;
	
		buf = BufferDescriptors;
		block = BufferBlocks;   
                
		/*
		 * link the buffers into a circular, doubly-linked list to
		 * initialize free list, and initialize the buffer headers. Still
		 * don't know anything about replacement strategy in this file.
		 */
		for (i = 0; i < Data_Descriptors; block += BLCKSZ, buf++, i++)
		{
			buf->bufNext = i + 1;
	
			CLEAR_BUFFERTAG(buf->tag);
			buf->buf_id = i;

			buf->data = MAKE_OFFSET(block);
			// Add blank data
			//*((char *) MAKE_PTR(buf->data)) = '#';
			*block = '#';

			buf->flags    = 0;
			buf->refcount = 0;
			buf->io_in_progress_lock = LWLockAssign();
			buf->cntx_lock = LWLockAssign();
			buf->cntxDirty = false;
			buf->wait_backend_id = 0;
		}

		/* close the circular queue */
                BufferDescriptors[NBuffers - 1].bufNext = -1;
		StrategyInitialize(true);
	}

}

// ******************************
// src/backend/storage/buffer/buf_table.c
// ******************************
#define HTABLE_SIZE 100
int htable[HTABLE_SIZE][HTABLE_SIZE];

/*
 * Estimate space needed for mapping hashtable
 * size is the desired hash table size (possibly more than NBuffers)
 */
int
BufTableShmemSize(int size)
{
	return 0;
}

/*
 * Initialize shmem hash table for mapping buffers
 * size is the desired hash table size (possibly more than NBuffers)
 */
void
InitBufTable(int size)
{
	memset(htable, -1, sizeof(htable));
}

/*
 * BufTableLookup
 * Lookup the given BufferTag; return buffer ID, or -1 if not found
 */
int
BufTableLookup(BufferTag *tagPtr)
{
	return htable[tagPtr->rnode.relNode][tagPtr->blockNum];
}

/*
 * BufTableInsert
 * Insert a hashtable entry for given tag and buffer ID
 */
void
BufTableInsert(BufferTag *tagPtr, int buf_id)
{
	htable[tagPtr->rnode.relNode][tagPtr->blockNum] = buf_id;
}

/*
 * BufTableDelete
 * Delete the hashtable entry for given tag (which must exist)
 */
void
BufTableDelete(BufferTag *tagPtr)
{
	htable[tagPtr->rnode.relNode][tagPtr->blockNum] = -1;
}

/*
 * ExceptionalCondition - Handles the failure of an Assert()
 */
int
ExceptionalCondition(char *conditionName,
					 char *errorType,
					 char *fileName,
					 int lineNumber)
{
	return 0;
}

TransactionId
GetTopTransactionId(void){
	return 0;
}
