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



// Read the latest VISIBLE record version associated with 'key'. If a visible
// version exists, sets '*result' equal to the value and returns true,
// otherwise returns false.
// 
// A tuple is visible to Xaction with ID XID if:
// Xmin must satisfy ALL of the following:
//   pg_log says it is committed
//   < XID (why?)
//   Not in process at start of curr Xaction
// Xmax must satisfy ONE of the following:
//   Is blank or aborted
//   Is > XID 
//   Was in process at start of curr Xaction
// 
bool MVStorage::Read(Key key, Value* result, uint64 mvcc_txn_id,
                     const map<uint64, TxnStatus>& pg_log_snapshot) {

  vector<row>& rows = data_[key];

  if (rows.size() == 0) {
    return false;
  }

//  Begin: Find visible routine
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
      if (xmin < mvcc_txn_id) {
        // AND xmax is one of the following
        if (xmax == 0
            || (pg_log_snapshot.count(xmax) && (pg_log_snapshot.at(xmax) == ABORTED))
            || xmax > mvcc_txn_id
            || (pg_log_snapshot.count(xmax) && (pg_log_snapshot.at(xmax) == INCOMPLETE))) {
          visible = true;
        }
      }
    }

    if (visible) {
      visible_tuple = &*r;
      found = true;
      break;
    }
  }

//
// End find visible
//

  if (!found) // should have found something because if there's at least one row, at least one tuple should be visible.
    DIE ("problem. Should have found something but didn't.");
  *result = visible_tuple->at(2);
  return true;
}

  // Insert a new record version (key, value), according to the postgreSQL-style
  // MVCC scheme.
void MVStorage::Write(Key key, Value value, uint64 mvcc_txn_id,
    const map<uint64, TxnStatus>& pg_log_snapshot) {
  vector<row>& rows = data_[key];
  // returns a reference and initializes rows;

  row* visible_tuple = NULL;
  vector<row>::iterator r;

  bool found = false;
  for (r = rows.begin(); r != rows.end(); r++) {
    uint64 xmin = r->at(0);
    uint64 xmax = r->at(1);
    bool visible = false;

    if (pg_log_snapshot.count(xmin) == 0) {
      // AND is less than current id
      if (xmin > mvcc_txn_id) {
        // AND one of the following about xmax is true
        if (xmax == 0
            || (pg_log_snapshot.count(xmax) && (pg_log_snapshot.at(xmax) == ABORTED))
            || xmax > mvcc_txn_id //TODO(syu): is this needed?
            || (pg_log_snapshot.count(xmax) && (pg_log_snapshot.at(xmax) == INCOMPLETE))) {
          visible = true;
        }
      }
    }

    if (visible) {
      visible_tuple = &*r;
      found = true;
      break;
    }
  } // end for 

  // If we found a visible tuple, then need to update its xmax
  if (found) {
    visible_tuple->at(2) = mvcc_txn_id;
  }

  // Regardless what we do, need to insert our own tuple
  row new_tuple = row(3, 0);
  new_tuple.at(0) = mvcc_txn_id;
  new_tuple.at(1) = 0;
  new_tuple.at(2) = value;

  rows.push_back(new_tuple);
}
