// Authors: Alexander Thomson (thomson@cs.yale.edu), Sherwin Yu (sherwin.yu@yale.edu)

#include "txn/txn_processor.h"

#include <set>

#include "txn/lock_manager.h"

// Thread & queue counts for StaticThreadPool initialization.
#define THREAD_COUNT 100
#define QUEUE_COUNT 10

TxnProcessor::TxnProcessor(CCMode mode)
  : mode_(mode), tp_(THREAD_COUNT, QUEUE_COUNT), next_unique_id_(1),
  next_mvcc_txn_id_(1) {
    if (mode_ == LOCKING_EXCLUSIVE_ONLY || mode_ == MVCC)
      lm_ = new LockManagerA(&ready_txns_);
    else if (mode_ == LOCKING)
      lm_ = new LockManagerB(&ready_txns_);

    // Start 'RunScheduler()' running as a new task in its own thread.
    tp_.RunTask(
        new Method<TxnProcessor, void>(this, &TxnProcessor::RunScheduler));
  }

TxnProcessor::~TxnProcessor() {
  if (mode_ == LOCKING_EXCLUSIVE_ONLY || mode_ == LOCKING)
    delete lm_;
}

void TxnProcessor::NewTxnRequest(Txn* txn) {
  // Atomically assign the txn a new number and add it to the incoming txn
  // requests queue.
  mutex_.Lock();
  txn->unique_id_ = next_unique_id_;
  next_unique_id_++;
  txn_requests_.Push(txn);
  mutex_.Unlock();
}

Txn* TxnProcessor::GetTxnResult() {
  Txn* txn;
  while (!txn_results_.Pop(&txn)) { // No result yet. Wait a bit before trying again (to reduce contention on // atomic queues).  sleep(0.000001);
  }
  return txn;
}

void TxnProcessor::RunScheduler() {
  switch (mode_) {
    case SERIAL:                 RunSerialScheduler(); break;
    case LOCKING:                RunLockingScheduler(); break;
    case LOCKING_EXCLUSIVE_ONLY: RunLockingScheduler(); break;
    case OCC:                    RunOCCScheduler(); break;
    case MVCC:                   RunMVCCScheduler(); break;
  }
}

void TxnProcessor::RunSerialScheduler() {
  Txn* txn;
  while (tp_.Active()) {
    // Get next txn request.
    if (txn_requests_.Pop(&txn)) {
      // Execute txn.
      ExecuteTxn(txn);

      // Commit/abort txn according to program logic's commit/abort decision.
      if (txn->Status() == COMPLETED_C) {
        ApplyWrites(txn);
        txn->status_ = COMMITTED;
      } else if (txn->Status() == COMPLETED_A) {
        txn->status_ = ABORTED;
      } else {
        // Invalid TxnStatus!
        DIE("Completed Txn has invalid TxnStatus: " << txn->Status());
      }

      // Return result to client.
      txn_results_.Push(txn);
    }
  }
}

void TxnProcessor::RunLockingScheduler() {
  Txn* txn;
  while (tp_.Active()) {
    // Start processing the next incoming transaction request.
    if (txn_requests_.Pop(&txn)) {
      int blocked = 0;

      // Request read locks.
      for (set<Key>::iterator it = txn->readset_.begin();
          it != txn->readset_.end(); ++it) {
        if (!lm_->ReadLock(txn, *it))
          blocked++;
      }

      // Request write locks.
      for (set<Key>::iterator it = txn->writeset_.begin();
          it != txn->writeset_.end(); ++it) {
        if (!lm_->WriteLock(txn, *it))
          blocked++;
      }

      // If all read and write locks were immediately acquired, this txn is
      // ready to be executed.
      if (blocked == 0)
        ready_txns_.push_back(txn);
    }

    // Process and commit all transactions that have finished running.
    while (completed_txns_.Pop(&txn)) {
      // Release read locks.
      for (set<Key>::iterator it = txn->readset_.begin();
          it != txn->readset_.end(); ++it) {
        lm_->Release(txn, *it);
      }
      // Release write locks.
      for (set<Key>::iterator it = txn->writeset_.begin();
          it != txn->writeset_.end(); ++it) {
        lm_->Release(txn, *it);
      }

      // Commit/abort txn according to program logic's commit/abort decision.
      if (txn->Status() == COMPLETED_C) {
        ApplyWrites(txn);
        txn->status_ = COMMITTED;
      } else if (txn->Status() == COMPLETED_A) {
        txn->status_ = ABORTED;
      } else {
        // Invalid TxnStatus!
        DIE("Completed Txn has invalid TxnStatus: " << txn->Status());
      }

      // Return result to client.
      txn_results_.Push(txn);
    }

    // Start executing all transactions that have newly acquired all their
    // locks.
    while (ready_txns_.size()) {
      // Get next ready txn from the queue.
      txn = ready_txns_.front();
      ready_txns_.pop_front();

      // Start txn running in its own thread.
      tp_.RunTask(new Method<TxnProcessor, void, Txn*>(
            this,
            &TxnProcessor::ExecuteTxn,
            txn));
    }
  }
}

void TxnProcessor::RunOCCScheduler() {

  Txn* txn;
  while (tp_.Active()) {
    // Get the next new transaction request (if one is pending) and start it running.
    if (txn_requests_.Pop(&txn)) {
      txn->occ_start_time_ = GetTime();
      tp_.RunTask(new Method<TxnProcessor, void, Txn*>(
            this,
            &TxnProcessor::ExecuteTxn,
            txn));
    }

    // Deal with all transactions that have finished running.
    while (completed_txns_.Pop(&txn)) {

      if (txn->Status() == COMPLETED_A) {
        txn->status_ = ABORTED;
        txn_results_.Push(txn);
        continue;
      }
      else if (txn->Status() != COMPLETED_C) {
        // Invalid TxnStatus!
        DIE("Completed Txn has invalid TxnStatus: " << txn->Status());
      }

      // Begin validation. txn status must be COMPLETED_C at this point.
      bool valid = true;
      set<Key>::iterator it;
  
      // Check both read/write sets and invalidate if existing record is newer
      for (it = txn->readset_.begin(); it != txn->readset_.end(); it++)
        if  (storage_.Timestamp(*it) > txn->occ_start_time_)
          valid = false;
      for (it = txn->writeset_.begin(); it != txn->writeset_.end(); it++)
        if (storage_.Timestamp(*it) > txn->occ_start_time_)
          valid = false;


     // if invalid, restart transaction
      if (!valid) {
        txn->status_ = INCOMPLETE;
        txn->occ_start_time_ = GetTime();
        tp_.RunTask(new Method<TxnProcessor, void, Txn*>(
              this,
              &TxnProcessor::ExecuteTxn,
              txn));
      } else {
        // Apply all writes.
        // Mark transaction as committed. (ApplyWrites does this for us, though it probably shouldn't)
        ApplyWrites(txn);
        txn_results_.Push(txn);
      }
    }

  }
}

void TxnProcessor::RunMVCCScheduler() {
  Txn* txn;
  while (tp_.Active()) {
    // Start processing the next incoming transaction request.
    // Request locks on transaction's writeset (but not readset), using the lock manager you implemented in part 1A.
    // If it has successfully acquired all locks, add it to the ready-to-run queue.
    if (txn_requests_.Pop(&txn)) {
      int blocked = 0;

      // Request write locks.
      for (set<Key>::iterator it = txn->writeset_.begin();
          it != txn->writeset_.end(); ++it) {
        if (!lm_->WriteLock(txn, *it))
          blocked++;
      }

      // If all read and write locks were immediately acquired, this txn is
      // ready to be executed.
      if (blocked == 0)
        ready_txns_.push_back(txn);
    }

    // Process and commit all transactions that have finished running.
    // Apply all writes.
    // Mark transaction as committed.
    // Release all write locks.
    // Remove this transaction's entry from pg_log. [IMPORTANT: This is different from how postgreSQL implements updates.]
    while (completed_txns_.Pop(&txn)) {
      // Release write locks.
      for (set<Key>::iterator it = txn->writeset_.begin();
          it != txn->writeset_.end(); ++it) {
        lm_->Release(txn, *it);
      }

      // Commit/abort txn according to program logic's commit/abort decision.
      if (txn->Status() == COMPLETED_C) {
        ApplyWritesMVCC(txn);
        txn->status_ = COMMITTED;
        pg_log_.erase(txn->mvcc_txn_id_);
      } else if (txn->Status() == COMPLETED_A) {
        pg_log_[txn->mvcc_txn_id_] = ABORTED;
        txn->status_ = ABORTED;
      } else {
        // Invalid TxnStatus!
        DIE("Completed Txn has invalid TxnStatus: " << txn->Status());
      }
      // Return result to client.
      txn_results_.Push(txn);
    }

    // Start executing all transactions that have newly acquired all their
    // locks.
    // Assign the txn a transactionID (using 'TxnProcessor::next_mvcc_txn_id_' and 'Txn::mvcc_txn_id_').
    // Insert this txn's transactionID into pg_log ('TxnProcessor::pg_log_').
    // Take a snapshot of the pg_log (storing it in the txn's 'Txn::pg_log_snapshot_').
    // Start transaction running in separate thread (but using 'TxnProcessor::ExecuteTxnMVCC' rather than 'TxnProcessor::ExecuteTxn').
    while (ready_txns_.size()) {
      txn = ready_txns_.front();
      ready_txns_.pop_front();

      // Assign transactionID.
      txn->mvcc_txn_id_ = next_mvcc_txn_id_++;

      // Insert this txn into pg_log.
      pg_log_[txn->mvcc_txn_id_] = INCOMPLETE;

      // Take pg_log snapshot.
      txn->pg_log_snapshot_ = pg_log_;

      // Start txn running in background thread.
      tp_.RunTask(new Method<TxnProcessor, void, Txn*>(
            this,
            &TxnProcessor::ExecuteTxnMVCC,
            txn));
    }
  }
}

void TxnProcessor::ExecuteTxn(Txn* txn) {
  // Read everything in from readset.
  for (set<Key>::iterator it = txn->readset_.begin();
      it != txn->readset_.end(); ++it) {
    // Save each read result iff record exists in storage.
    Value result;
    if (storage_.Read(*it, &result))
      txn->reads_[*it] = result;
  }

  // Also read everything in from writeset.
  for (set<Key>::iterator it = txn->writeset_.begin();
      it != txn->writeset_.end(); ++it) {
    // Save each read result iff record exists in storage.
    Value result;
    if (storage_.Read(*it, &result))
      txn->reads_[*it] = result;
  }

  // Execute txn's program logic.
  txn->Run();

  // Hand the txn back to the RunScheduler thread.
  completed_txns_.Push(txn);
}

void TxnProcessor::ExecuteTxnMVCC(Txn* txn) {
  // Read everything in from readset.
  for (set<Key>::iterator it = txn->readset_.begin();
      it != txn->readset_.end(); ++it) {
    // Save each read result iff record exists in storage.
    Value result;
    if (mv_storage_.Read(*it, &result, txn->mvcc_txn_id_,
          txn->pg_log_snapshot_)) {
      txn->reads_[*it] = result;
    }
  }

  // Also read everything in from writeset.
  for (set<Key>::iterator it = txn->writeset_.begin();
      it != txn->writeset_.end(); ++it) {
    // Save each read result iff record exists in storage.
    Value result;
    if (mv_storage_.Read(*it, &result, txn->mvcc_txn_id_,
          txn->pg_log_snapshot_)) {
      txn->reads_[*it] = result;
    }
  }

  // Execute txn's program logic.
  txn->Run();

  // Hand the txn back to the RunScheduler thread.
  completed_txns_.Push(txn);
}

void TxnProcessor::ApplyWrites(Txn* txn) {
  // Write buffered writes out to storage.
  for (map<Key, Value>::iterator it = txn->writes_.begin();
      it != txn->writes_.end(); ++it) {
    storage_.Write(it->first, it->second);
  }

  // Set status to committed.
  txn->status_ = COMMITTED;
}

void TxnProcessor::ApplyWritesMVCC(Txn* txn) {
  // Write buffered writes out to storage.
  for (map<Key, Value>::iterator it = txn->writes_.begin();
      it != txn->writes_.end(); ++it) {
    mv_storage_.Write(it->first, it->second, txn->mvcc_txn_id_,
        txn->pg_log_snapshot_);
  }
}


