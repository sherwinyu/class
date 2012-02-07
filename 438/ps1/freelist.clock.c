/*-------------------------------------------------------------------------
 *
 * freelist.c
 *	  routines for manipulating the buffer pool's replacement strategy.
 *
 * The name "freelist.c" is now a bit of a misnomer, since this module
 * controls not only the list of free buffers per se, but the entire
 * mechanism for looking up existing shared buffers and the strategy
 * for choosing replacement victims when needed.
 *
 * Note: all routines in this file assume that the BufMgrLock is held
 * by the caller, so no synchronization is needed.
 *
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/storage/buffer/freelist.c,v 1.49.4.2 2005/03/03 16:47:43 tgl Exp $
 *
 *-------------------------------------------------------------------------
 */
#include "postgres.h"

#include <time.h>

#include "access/xact.h"
#include "storage/buf_internals.h"
#include "storage/bufmgr.h"


/*
 * We have a cache directory block (CDB) for every file page currently being
 * tracked by the strategy module.  There can be more CDBs than there are
 * actual shared buffers, allowing pages no longer in cache to still be
 * tracked.
 */
typedef struct
{
	int		prev;		/* list links */
	int		next;
	BufferTag	buf_tag;	/* page identifier */
	int		buf_id;		/* currently assigned data buffer, or -1 */
} BufferStrategyCDB;

/*
 * The shared strategy control information.
 */
typedef struct
{
	int	listUnusedCDB;			/* All unused StrategyCDB */
	int	listHead;	/* CDB lists */
	int	listTail;
	int	listSize;
	Buffer	listFreeBuffers;		/* List of unused buffers */

	long	num_lookup;			/* Some hit statistics */
	long num_hit;
	time_t	stat_report;

	/* Array of CDB's starts here */
	BufferStrategyCDB cdb[1];	/* VARIABLE SIZE ARRAY */
} BufferStrategyControl;

/* GUC variable: time in seconds between statistics reports */
int DebugSharedBuffers = 0;
int * refbits;
int * clockPtr;

/* Pointers to shared state */
static BufferStrategyControl *StrategyControl = NULL;
static BufferStrategyCDB     *StrategyCDB     = NULL;

/* Backend-local state about whether currently vacuuming */
static bool          strategy_hint_vacuum = false;
static TransactionId strategy_vacuum_xid;


#define T1_LENGTH (StrategyControl->listSize)


/*
 * Macro to remove a CDB from whichever list it currently is on
 */
#define STRAT_LIST_REMOVE(cdb) \
do { \
	if ((cdb)->prev < 0)						\
		StrategyControl->listHead = (cdb)->next;	\
	else								\
		StrategyCDB[(cdb)->prev].next = (cdb)->next;		\
	if ((cdb)->next < 0)						\
		StrategyControl->listTail = (cdb)->prev;	\
	else								\
		StrategyCDB[(cdb)->next].prev = (cdb)->prev;		\
	StrategyControl->listSize--;			\
} while(0)




/*
 * Macro to add a CDB to the tail of a list (MRU position)
 */

#define STRAT_MRU_INSERT(cdb) \
do { \
	if (StrategyControl->listTail < 0)				\
	{								\
		(cdb)->prev = (cdb)->next = -1;				\
		StrategyControl->listHead =			\
			StrategyControl->listTail =		\
			((cdb) - StrategyCDB);				\
	}								\
	else								\
	{								\
		(cdb)->next = -1;					\
		(cdb)->prev = StrategyControl->listTail;		\
		StrategyCDB[StrategyControl->listTail].next =	\
			((cdb) - StrategyCDB);				\
		StrategyControl->listTail =			\
			((cdb) - StrategyCDB);				\
	}								\
	StrategyControl->listSize++;				\
} while(0)

/*
 * Macro to add a CDB to the head of a list (LRU position)
 */
#define STRAT_LRU_INSERT(cdb) \
do { \
	if (StrategyControl->listHead < 0)				\
	{								\
		(cdb)->prev = (cdb)->next = -1;				\
		StrategyControl->listHead =			\
			StrategyControl->listTail =		\
			((cdb) - StrategyCDB);				\
	}								\
	else								\
	{								\
		(cdb)->prev = -1;					\
		(cdb)->next = StrategyControl->listHead;		\
		StrategyCDB[StrategyControl->listHead].prev =	\
			((cdb) - StrategyCDB);				\
		StrategyControl->listHead =			\
			((cdb) - StrategyCDB);				\
	}								\
	StrategyControl->listSize++;				\
} while(0)

/*
 * Function to insert a CDB into the list, ordered by buf_id
 * Used for implementing CLOCK
 *
 * cdb - a pointer to a BufferStrategyCDB to be inserted.
 */

/*
void ordered_insert(BufferStrategyCDB * cdb) {
  if (StrategyControl->listHead < 0) {
    (cdb)->prev = (cdb)->next = -1;
    StrategyControl->listHead =
      StrategyControl->listTail =
      ((cdb) - StrategyCDB);
  }
  else {
    BufferStrategyCDB * curCdb = &StrategyCDB[StrategyControl->listHead];
    assert(curCdb->prev == -1);

    while (curCdb->next != -1 && curCdb->buf_id < cdb->buf_id) {
      curCdb = &StrategyCDB[curCdb->next];
    } // when loop exits; curCdb will be pointing to node BEFORE which we insert cdb
    // when there is a single element, curCdb->next == -1
    // when the first elements' buf_id > cdb->buf_id, same thing happens and curCdb will point to head

    cdb->next = curCdb - StrategyCDB;

    (cdb)->prev = curCdb - StrategyCDB;
    (cdb)->next = curCdb->next;
    curCdb->next = (cdb) - StrategyCDB;
    if (cdb->next != -1)
      StrategyCDB[cdb->next].prev = (cdb) - StrategyCDB;
  }
}

*/
/*
#define STRAT_ORDERED_INSERT(cdb) \
  do { \
    if (StrategyControl->listHead < 0) { \
		(cdb)->prev = (cdb)->next = -1;				\
		StrategyControl->listHead =			\
			StrategyControl->listTail =		\
			((cdb) - StrategyCDB);				\
    } \
    else { \
      BufferStrategyCDB * curCdb = &StrategyCDB[StrategyControl->listHead]; \
      do { \
        curCdb = &StrategyCDB[curCdb->next]; \
      } while(curCdb->next != -1 && curCdb->buf_id < (cdb)->buf_id); \
      (cdb)->prev = curCdb - StrategyCDB; \
      (cdb)->next = curCdb->next; \
      curCdb->next = (cdb) - StrategyCDB; \
      if (cdb->next != -1) \
        StrategyCDB[cdb->next].prev = (cdb) - StrategyCDB; \
    } \
  } while(0)
  */


/*
 * Printout for use when DebugSharedBuffers is enabled
 */
static void
StrategyStatsDump(void)
{
	time_t	now = time(NULL);

	if (StrategyControl->stat_report + DebugSharedBuffers < now)
	  {
	  long t1_hit;
	  int  id, t1_clean;
	  ErrorContextCallback *errcxtold;

	  id = StrategyControl->listHead;
	  t1_clean = 0;
	  while (id >= 0)
		{
		  if (!(BufferDescriptors[StrategyCDB[id].buf_id].flags & BM_DIRTY))
			t1_clean++;
			id = StrategyCDB[id].next;
		}

	  if (StrategyControl->num_lookup == 0)
		t1_hit = 0;
	  else
		{
		  t1_hit = (StrategyControl->num_hit * 100 /
					StrategyControl->num_lookup);
		}

		errcxtold = error_context_stack;
		error_context_stack = NULL;
		elog(DEBUG1, "Q STAT: T1len=%5d ", T1_LENGTH);
		elog(DEBUG1, "Q T1hit=%4ld%% ", t1_hit);
		elog(DEBUG1, "Q clean buffers: T1= %5d", t1_clean);
		error_context_stack = errcxtold;

		StrategyControl->num_lookup = 0;
		StrategyControl->num_hit = 0;
		StrategyControl->stat_report = now;
	  }
}

/*
 * StrategyBufferLookup
 *
 *	Lookup a page request in the cache directory. A buffer is only
 *	returned for cache hit.
 *
 *	recheck indicates we are rechecking after I/O wait; do not change
 *	internal status in this case.
 *
 *	*cdb_found_index is set to the index of the found CDB, or -1 if none.
 *	This is not intended to be used by the caller, except to pass to
 *	StrategyReplaceBuffer().
 */
BufferDesc *
StrategyBufferLookup(BufferTag *tagPtr, bool recheck, int *cdb_found_index)
{
	BufferStrategyCDB *cdb;

	/* Optional stats printout */
	if (DebugSharedBuffers > 0)
		StrategyStatsDump();

	/*
	 * Count lookups
	 */
	StrategyControl->num_lookup++;

	/*
	 * Lookup the block in the shared hash table
	 */
	*cdb_found_index = BufTableLookup(tagPtr);

	/*
	 * Done if complete CDB lookup miss
	 */
	if (*cdb_found_index < 0)
		return NULL;

	/*
	 * We found a CDB
	 */
	cdb = &StrategyCDB[*cdb_found_index];

	/*
	 * Count hits
	 */
	StrategyControl->num_hit++;

	/*
	 * If this is a hit, move to MRU end.
	 */

		// STRAT_LIST_REMOVE(cdb);
		// STRAT_MRU_INSERT(cdb);
		// STRAT_ORDERED_INSERT(cdb);

		return &BufferDescriptors[cdb->buf_id];

	/*
	 * cache miss to the bufmgr (not reached).
	 */
	return NULL;

}


/*
 * StrategyGetBuffer
 *
 *	Called by the bufmgr to get the next candidate buffer to use in
 *	BufferAlloc(). The only hard requirement BufferAlloc() has is that
 *	this buffer must not currently be pinned.
 *
 *	*cdb_replace_index is set to the index of the candidate CDB, or -1 if
 *	none (meaning we are using a previously free buffer).  This is not
 *	intended to be used by the caller, except to pass to
 *	StrategyReplaceBuffer().
 */
BufferDesc *
StrategyGetBuffer(int *cdb_replace_index)
{
	int			cdb_id;
	BufferDesc *buf;

	if (StrategyControl->listFreeBuffers < 0)
	{
		BufferDesc * buf;
		buf = BufferDescriptors;

		for (;; *clockPtr = (*clockPtr + 1) % NBuffers) {
			// assert(buffer is empty!)
			buf = &BufferDescriptors[*clockPtr] ;
			if (buf->refcount != 0)
			  continue;
			if (!refbits[*clockPtr]) { // evict (currently it's 0)
			  StrategyBufferLookup(& (buf->tag), false, cdb_replace_index);
			  refbits[*clockPtr] = 1;
			  *clockPtr = (*clockPtr + 1) % NBuffers;

			  return buf;
			}
			else { // set the refbit to 0 because currently it's 1
			  refbits[*clockPtr] = 0;
			}
		}

		//
		//
		//
        // --------- my code goes above this line!------------------------------ 
		//
		//
		/*
		 * We should take the first unpinned buffer from the list.
		 */
		/*
		cdb_id = StrategyControl->listHead;
		while (cdb_id >= 0)
		{
			buf = &BufferDescriptors[StrategyCDB[cdb_id].buf_id];
			if (buf->refcount == 0)
			{
				*cdb_replace_index = cdb_id;
				return buf;
			}
			cdb_id = StrategyCDB[cdb_id].next;
		}
		*/

		/*
		 * No unpinned buffers at all!!!
		 */
		elog(ERROR, "no unpinned buffers available");
	}
	else
	{
		/* There is a completely free buffer available - take it */

		/*
		 * Note: This code uses the side effect that a free buffer can
		 * never be pinned or dirty and therefore the call to
		 * StrategyReplaceBuffer() will happen without the bufmgr
		 * releasing the bufmgr-lock in the meantime. That means, that
		 * there will never be any reason to recheck. Otherwise we would
		 * leak shared buffers here!
		 */
		*cdb_replace_index = -1;
		buf = &BufferDescriptors[StrategyControl->listFreeBuffers];

		StrategyControl->listFreeBuffers = buf->bufNext;
		buf->bufNext = -1;

		/* Buffer in freelist cannot be pinned */
		Assert(buf->refcount == 0);
		Assert(!(buf->flags & BM_DIRTY));

		return buf;
	}

	/* not reached */
	return NULL;
}


/*
 * StrategyReplaceBuffer
 *
 *	Called by the buffer manager to inform us that he flushed a buffer
 *	and is now about to replace the content. Prior to this call,
 *	the cache algorithm still reports the buffer as in the cache. After
 *	this call we report the new block, even if IO might still need to
 *	be done to bring in the new content.
 *
 *	cdb_found_index and cdb_replace_index must be the auxiliary values
 *	returned by previous calls to StrategyBufferLookup and StrategyGetBuffer.
 *
 *      Comment (for CS438/538 project): As you can see, you may ignore found_index for this project.
 *              Also notice, that replace_index is what YOUR code returned in
 *              StrategyGetBuffer.
 */
void
StrategyReplaceBuffer(BufferDesc *buf, BufferTag *newTag, int cdb_found_index, int cdb_replace_index)
{
	BufferStrategyCDB *cdb_found;
	BufferStrategyCDB *cdb_replace;

	/*
	 * This was a complete cache miss, so we need to create a new CDB.
	 * There should be unused one available for use
	 */
	if (StrategyControl->listUnusedCDB >= 0)
	{
		cdb_found = &StrategyCDB[StrategyControl->listUnusedCDB];
		StrategyControl->listUnusedCDB = cdb_found->next;
	}
	else
	{
		elog(PANIC, "no CDBs to use!");
	}

	/* Set the CDB's buf_tag and insert it into the hash table */
	cdb_found->buf_tag = *newTag;
	BufTableInsert(&(cdb_found->buf_tag), (cdb_found - StrategyCDB));

	if (cdb_replace_index >= 0)
	{
		/*
		 * The buffer was formerly in a the buffer list, move its CDB to the unused list
		 */
		cdb_replace = &StrategyCDB[cdb_replace_index];

		Assert(cdb_replace->buf_id == buf->buf_id);
		Assert(BUFFERTAGS_EQUAL(cdb_replace->buf_tag, buf->tag));

		BufTableDelete(&(cdb_replace->buf_tag));
		STRAT_LIST_REMOVE(cdb_replace);
		cdb_replace->next = StrategyControl->listUnusedCDB;
		StrategyControl->listUnusedCDB = cdb_replace_index;

		/* And clear its block reference */
		cdb_replace->buf_id = -1;
	}
	else
	{
		/* We are satisfying it with an unused buffer */
	}

	/* Assign the buffer id to the new CDB */
	cdb_found->buf_id = buf->buf_id;

	/* Insert at appropriate end of linked list */
	// STRAT_MRU_INSERT(cdb_found);

}


/*
 * StrategyInvalidateBuffer
 *
 *	Called by the buffer manager to inform us that a buffer content
 *	is no longer valid. We simply throw away any eventual existing
 *	buffer hash entry and move the CDB and buffer to the free lists.
 *
 */
void
StrategyInvalidateBuffer(BufferDesc *buf)
{
	int			cdb_id;
	BufferStrategyCDB *cdb;

	/* The buffer cannot be dirty or pinned */
	Assert(!(buf->flags & BM_DIRTY) || !(buf->flags & BM_VALID));
	Assert(buf->refcount == 0);

	/*
	 * Lookup the cache directory block for this buffer
	 */
	cdb_id = BufTableLookup(&(buf->tag));
	if (cdb_id < 0)
		elog(ERROR, "buffer %d not in buffer hash table", buf->buf_id);
	cdb = &StrategyCDB[cdb_id];

	/*
	 * Remove the CDB from the hashtable and the queue it is currently
	 * on.
	 */
	BufTableDelete(&(cdb->buf_tag));
	STRAT_LIST_REMOVE(cdb);

	/*
	 * Clear out the CDB's buffer tag and association with the buffer and
	 * add it to the list of unused CDB's
	 */
	CLEAR_BUFFERTAG(cdb->buf_tag);
	cdb->buf_id = -1;
	cdb->next = StrategyControl->listUnusedCDB;
	StrategyControl->listUnusedCDB = cdb_id;

	/*
	 * Clear out the buffer's tag and add it to the list of currently
	 * unused buffers.	We must do this to ensure that linear scans of the
	 * buffer array don't think the buffer is valid.
	 */
	CLEAR_BUFFERTAG(buf->tag);
	buf->flags &= ~(BM_VALID | BM_DIRTY);
	buf->cntxDirty = false;
	buf->bufNext = StrategyControl->listFreeBuffers;
	StrategyControl->listFreeBuffers = buf->buf_id;

}

/*
 * StrategyHintVacuum -- tell us whether VACUUM is active
 */
void
StrategyHintVacuum(bool vacuum_active)
{
/*   Comment (for CS438/538 project): Ignore this routine. */
	strategy_hint_vacuum = vacuum_active;
	strategy_vacuum_xid = GetTopTransactionId();
}

/*
 * StrategyDirtyBufferList
 *
 * Returns a list of dirty buffers, in priority order for writing.
 * Note that the caller may choose not to write them all.
 *
 * The caller must beware of the possibility that a buffer is no longer dirty,
 * or even contains a different page, by the time he reaches it.  If it no
 * longer contains the same page it need not be written, even if it is (again)
 * dirty.
 *
 * Buffer pointers are stored into buffers[], and corresponding tags into
 * buftags[], both of size max_buffers.  The function returns the number of
 * buffer IDs stored.
 */
int
StrategyDirtyBufferList(BufferDesc **buffers, BufferTag *buftags, int max_buffers)
{
	int			num_buffer_dirty = 0;
	int			cdb_id_t1;
	int			buf_id;
	BufferDesc *buf;

	/*
	 * Traverse the T1 LRU to MRU  and add all
	 * dirty buffers found in that order to the list.
	 */
	cdb_id_t1 = StrategyControl->listHead;

	while (cdb_id_t1 >= 0)
	{
		buf_id = StrategyCDB[cdb_id_t1].buf_id;
		buf = &BufferDescriptors[buf_id];

		if (buf->flags & BM_VALID)
		{
			if ((buf->flags & BM_DIRTY) || (buf->cntxDirty))
			{
				buffers[num_buffer_dirty] = buf;
				buftags[num_buffer_dirty] = buf->tag;
				num_buffer_dirty++;
				if (num_buffer_dirty >= max_buffers)
					break;
			}
		}

		cdb_id_t1 = StrategyCDB[cdb_id_t1].next;

	}

	return num_buffer_dirty;
}


/*
 * StrategyShmemSize
 *
 * estimate the size of shared memory used by the freelist-related structures.
 *
 * Comment (for CS438/538 project): This is done for you.
 */
int
StrategyShmemSize(void)
{
	/* A1out list can hold 50% of NBuffers, per Johnson and Shasha */
	int			nCDBs = NBuffers + NBuffers / 2;
	int			size = 0;

	/* size of CDB lookup hash table */
	size += BufTableShmemSize(nCDBs);

	/* size of the shared replacement strategy control block */
	size += MAXALIGN(sizeof(BufferStrategyControl));

	/* size of the CDB directory */
	size += MAXALIGN(nCDBs * sizeof(BufferStrategyCDB));

	return size;
}

/*
 * StrategyInitialize -- initialize the buffer cache replacement
 *		strategy.
 *
 * Assume: All of the buffers are already building a linked list.
 *		Only called by postmaster and only during initialization.
 *
 * Comment (for CS438/538 project): This is done for you.
 */
void
StrategyInitialize(bool init)
{
	/* CDB list can hold 50% of NBuffers, per Johnson and Shasha */
	int	nCDBs = NBuffers + NBuffers / 2;
	bool	found;
	int	i;

	bool otherFound;
	refbits = (int *) ShmemInitStruct("Refbit vector", sizeof(int *) * NBuffers, &otherFound);
	clockPtr = (int *) ShmemInitStruct("Clock pointer", sizeof(int *) * 1, &otherFound);
	*clockPtr = 0;
	int j;
	for (j = 0; j < NBuffers; j++)
	  refbits[j] = 1;

	/*
	 * Initialize the shared CDB lookup hashtable
	 */
	InitBufTable(nCDBs);

	/*
	 * Get or create the shared strategy control block and the CDB's
	 */
	StrategyControl = (BufferStrategyControl *)
		ShmemInitStruct("Buffer Strategy Status",
						sizeof(BufferStrategyControl) +
						sizeof(BufferStrategyCDB) * (nCDBs - 1),
						&found);
	StrategyCDB = &(StrategyControl->cdb[0]);

	if (!found)
	{
		/*
		 * Only done once, usually in postmaster
		 */
		Assert(init);

		/*
		 * Grab the whole linked list of free buffers for our strategy. We
		 * assume it was previously set up by InitBufferPool().
		 */
		StrategyControl->listFreeBuffers = 0;

		/*
		 * Initialize list and stats to be empty
		 */

		StrategyControl->listHead = -1;
		StrategyControl->listTail = -1;
		StrategyControl->listSize = 0;
		StrategyControl->num_hit = 0;

		StrategyControl->num_lookup = 0;
		StrategyControl->stat_report = 0;

		/*
		 * All CDB's are linked as the listUnusedCDB
		 */
		for (i = 0; i < nCDBs; i++)
		{
			StrategyCDB[i].next = i + 1;
			CLEAR_BUFFERTAG(StrategyCDB[i].buf_tag);
			StrategyCDB[i].buf_id = -1;
		}
		StrategyCDB[nCDBs - 1].next = -1;
		StrategyControl->listUnusedCDB = 0;
	}
	else
		Assert(!init);
}
