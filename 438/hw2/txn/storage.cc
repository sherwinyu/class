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

bool MVStorage::Read(Key key, Value* result, uint64 mvcc_txn_id,
                     const map<uint64, TxnStatus>& pg_log_snapshot) {
  printf("%p\n", &data_[key]);
  vector<vector<uint64>>& rows = data_[key];
  for(int i = 0; i < rows.siz(); i++) {
    row = rows[i]
  }

  // Read the latest VISIBLE record version associated with 'key'. If a visible
  // version exists, sets '*result' equal to the value and returns true,
  // otherwise returns false.
  if (row == 0) {
    

  }

  deque<LockRequest>*& lock_requests = lock_table_[key]; 


  printf("%ld\t%ld\t%ld\n", data_[key][0], data_[key][1], data_[key][2]);
  return false;
}

void MVStorage::Write(Key key, Value value, uint64 mvcc_txn_id,
                      const map<uint64, TxnStatus>& pg_log_snapshot) {
  // CPSC 438/538:
  //
  // Implement this method!
}

