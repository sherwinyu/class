#include "txn/storage.h"

bool Storage::Read(Key key, Value* result) {
  if (data_.count(key)) {
    *result = data_[key];
    return true;
  } else {
    return false;
  }
}

void Storage::Write(Key key, Value value) {
  data_[key] = value;
  timestamps_[key] = GetTime();
}

double Storage::Timestamp(Key key) {
  if (timestamps_.count(key) == 0)
    return 0;
  return timestamps_[key];
}

typedef vector<uint64> row;


/*
A tuple is visible to Xaction with ID XID if:
Xmin must satisfy ALL of the following:
  pg_log says it is committed
  < XID (why?)
  Not in process at start of curr Xaction
Xmax must satisfy ONE of the following:
  Is blank or aborted
  Is > XID 
  Was in process at start of curr Xaction
*/

// Read the latest VISIBLE record version associated with 'key'. If a visible
// version exists, sets '*result' equal to the value and returns true,
// otherwise returns false.
bool MVStorage::Read(Key key, Value* result, uint64 mvcc_txn_id,
                     const map<uint64, TxnStatus>& pg_log_snapshot) {
  vector<row>& rows = data_[key];
  printf("%p size: %d\n", &rows, (int) rows.size());

  if (rows.size() == 0) {
    return false;
  }

//  given a rows vector of <xmin, xmax, data> tuples, a pg_log_snapshot, and a
//  mvcc_txn_id:
//  return a ref to the visible row
  
  row* visible_tuple = NULL;
  vector<row>::iterator r;
  // For every row for the key:
  bool found = false;
  for (r = rows.begin(); r != rows.end(); r++) {
    uint64 xmin = r->at(0);
    uint64 xmax = r->at(1);
    bool visible = false;

    // is completed; not in pg_log means not aborted and not commited
    if (pg_log_snapshot.count(xmin) == 0) { 
      // AND is less than current id
      if (xmin < mvcc_txn_id) 
      {
        // AND xmax is one of the following
        if (xmax == -1
            || (pg_log_snapshot.count(xmax) && pg_log_snapshot[xmax] == ABORTED)
            || xmax > mvcc_txn_id
            || (pg_log_snapshot.count(xmax) && pg_log_snapshot[xmax] == INCOMPLETE)) {
          visible = true;
        }
      }
    }

    if (visible) {
      visible_tuple = r;
      found = true;
      break;
    }
    printf("row #%d. has 3 tuples: %d\n", (int) (r-rows.begin()),(int) r->size());
  }

  if (!found) // should have found something because if there's at least one row, at least one tuple should be visible.
    DIE ("problem. Should have found something but didn't.");
  *result = visible_tuple->at(2);
  return true;
  // printf("%ld\t%ld\t%ld\n", data_[key][0], data_[key][1], data_[key][2]);
}

void MVStorage::Write(Key key, Value value, uint64 mvcc_txn_id,
                      const map<uint64, TxnStatus>& pg_log_snapshot) {
  // CPSC 438/538:
  //
  // Implement this method!
}

