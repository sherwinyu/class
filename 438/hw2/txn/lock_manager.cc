// Author: Alexander Thomson (thomson@cs.yale.edu)
//
// Lock manager implementing deterministic two-phase locking as described in
// 'The Case for Determinism in Database Systems'.

#include "txn/lock_manager.h"
#include <iostream>
#include <deque>
#include <map>
#include <vector>

#include "txn/common.h"
using namespace std;
using std::deque;


LockManagerA::LockManagerA(deque<Txn*>* ready_txns) {
  ready_txns_ = ready_txns;
}

bool LockManagerA::WriteLock(Txn* txn, const Key& key) {
  
  deque<LockRequest>*& lock_requests = lock_table_[key];

  if (lock_requests == 0) {
    lock_requests = new deque<LockRequest>(1, LockRequest(EXCLUSIVE, txn));
    return true;
  }
  if ((*lock_requests).size() == 0) {
    (*lock_requests).push_back(LockRequest(EXCLUSIVE, txn));
    return true;
  }

  txn_waits_[txn]++;
  (*lock_requests).push_back(LockRequest(EXCLUSIVE, txn));
  return false;
}

bool LockManagerA::ReadLock(Txn* txn, const Key& key) {
  // Since Part 1A implements ONLY exclusive locks, calls to ReadLock can
  // simply use the same logic as 'WriteLock'.
  return WriteLock(txn, key);
}

void LockManagerA::Release(Txn* txn, const Key& key) {
  // TODO(syu): when do we free the lock_request queue that was created in
  // WriteLock?
  
  deque<LockRequest>*& lock_requests = lock_table_[key];
  deque<LockRequest>::iterator it;

  for (it = (*lock_requests).begin();
    it != (*lock_requests).end() && (*it).txn_ != txn ; ++it); 

  if (it == (*lock_requests).end()) {
    DIE("Release called with invalid (txn,key): txn not found in key's entry in locktable");
  }

  if (it == (*lock_requests).begin()) {
    // printf("Erasing %p\n", it->txn_);
    // printf("lock_requests size = %d", (int) lock_requests->size());
    (*lock_requests).erase(it);

    // pushback txn to ready if size > 0 and the next transaction is waiting for no more
    // locks
    if (lock_requests->size() > 0) { // need to check for > 0 because begin() could be a old pointer
      Txn* next_txn = lock_requests->begin()->txn_;
      txn_waits_[next_txn]--;
      // printf("txn_waits[next_txn] = %d\n", txn_waits_[next_txn]);
      if  (txn_waits_[next_txn] == 0) {
        // printf("Pushing back %p to ready txns\n", next_txn);
        ready_txns_->push_back(next_txn);
      }
    }
  }
  else { // cancelled
    printf("Erasing %p\n", it->txn_);
    (*lock_requests).erase(it);
  }
/*
  */

}

LockMode LockManagerA::Status(const Key& key, vector<Txn*>* owners) {
  deque<LockRequest>*& lock_requests = lock_table_[key];

  if (lock_requests == 0 || lock_requests->size() == 0) {
    *owners = vector<Txn*>(0);
    return UNLOCKED;
  }
  *owners = vector<Txn*>(0);
  owners->push_back(lock_requests->begin()->txn_);
  return lock_requests->begin()->mode_;
}

LockManagerB::LockManagerB(deque<Txn*>* ready_txns) {
  ready_txns_ = ready_txns;
}

/*
 * WriteLock appends a LockRequest for a exclusive lock to the end of the
 * queue for lock_table_[key].
 *
 * Returns true if:
 *  lock_table_[key] is empty or doesn't exist yet
 *  
 *  False otherwise.
 *
 *  if it returns false, txn_waits_[txn] is incremented.
 *  
 */
bool LockManagerB::WriteLock(Txn* txn, const Key& key) {
  
  deque<LockRequest>*& lock_requests = lock_table_[key]; 

  if (lock_requests == 0) {
    lock_requests = new deque<LockRequest>(1, LockRequest(EXCLUSIVE, txn));
    return true;
  }
  if ((*lock_requests).size() == 0) {
    (*lock_requests).push_back(LockRequest(EXCLUSIVE, txn));
    return true;
  }
  
  txn_waits_[txn]++;
  (*lock_requests).push_back(LockRequest(EXCLUSIVE, txn));
  return false;
}

/*
 * ReadLock appends a LockRequest for a shared lock to teh end of the queue for
 * lock_table_[key].
 *
 * Returns true if 
 *  lock_table_[key] is empty OR
 *  lock_table_[key] doesn't exist yet OR
 *  lock_table_[key] has only shared keys
 * False otherwise .
 * 
 * If it returns false, txn_waits_[txn] is incremented.
 */
bool LockManagerB::ReadLock(Txn* txn, const Key& key) {
  
  deque<LockRequest>*& lock_requests = lock_table_[key]; 
 
  // if lock_requests does not exist yet
  if (lock_requests == 0) {
    lock_requests = new deque<LockRequest>(1, LockRequest(SHARED, txn));
    return true;
  }
  // or if lock_requests is empty
  if ((*lock_requests).size() == 0) {
    (*lock_requests).push_back(LockRequest(SHARED, txn));
    return true;
  }
  // Otherwise, a non empty queue exists
  // Append the new read lock request to the queue
  lock_requests->push_back(LockRequest(SHARED, txn));

  // Then find the location of the first exclusive lock, if any
  unsigned i = 0;
  for (; i < lock_requests->size() && (*lock_requests)[i].mode_ != EXCLUSIVE; i++);

  printf("First exclusive lock found at %d, size is %d\n", i, (int) lock_requests->size());

  // If we got to the end without finding it, then the queue contains no
  // exclusive locks so the lock is immediately available. Otherwise, return
  // false.
  if (i == lock_requests->size()) 
    return true;

  txn_waits_[txn]++;
  return false;
}

/*
 * 
 *  Loops through the lock_table_[key] queue until it finds the LockRequest with
 *  the corresponding txn.
 *  If the current LockRequest is a Exclusive Lock
 *      Then erase the lockrequest
 *    If the current LockRequest was found at the beginning
 *      decrement the waits on the next batch of requests
 *  If the current LockRequest is a Shared Lock
 *    Erase the LockRequest
 *    If the current LockRequest was found at the beginning and the next one is
 *    an exclusive lock (this was the last shared lock request)
 *      decrement the waits on the next batch of requests
 *
  // Releases lock held by 'txn' on 'key', or cancels any pending request for
  // a lock on 'key' by 'txn'. If 'txn' held an EXCLUSIVE lock on 'key' (or was
  // the sole holder of a SHARED lock on 'key'), then the next request(s) in the
  // request queue is granted. If the granted request(s) corresponds to a
  // transaction that has now acquired ALL of its locks, that transaction is
  // appended to the 'ready_txns_' queue.
  //
  // CPSC 438/538:
  // IMPORTANT NOTE: In order to know WHEN a transaction is ready to run, you
  // may need to track its lock acquisition progress during the lock request
  // process.
  // (Hint: Use 'LockManager::txn_waits_' defined below.)
*/
void LockManagerB::Release(Txn* txn, const Key& key) {
  
  deque<LockRequest>*& lock_requests = lock_table_[key]; 

  // Move forward until we find the txn we wish to release
  deque<LockRequest>::iterator it;
  bool passedARead;
  bool passedAWrite;
  for (it = (*lock_requests).begin(); it != (*lock_requests).end() && (*it).txn_ != txn ; ++it) {
    if (it->mode_ == SHARED)    
      passedARead = true;
    if (it->mode_ == EXCLUSIVE)    
      passedAWrite = true;
  } 

  if (it == (*lock_requests).end()) {
    DIE("Release called with invalid (txn,key): txn not found in key's entry in locktable");
  }

  bool lastLock = false;

  if (it + 1 < lock_requests->end()) {
    LockMode mode = it->mode_;
    LockMode nextMode = (it + 1)->mode_;

  // We need to decrement the wait counts on the next batch of lock requests if
  // either of these conditions are true:
  if (mode == EXCLUSIVE && !passedARead && !passedAWrite)
    lastLock = true;
  if (mode == SHARED && nextMode == EXCLUSIVE && !passedARead && !passedAWrite)
    lastLock = true;
  if (mode == EXCLUSIVE && nextMode == SHARED && passedARead && !passedAWrite)
    lastLock = true;
  }

  if (lastLock && lock_requests->size() > 1) {
    // We need to differentiate between whether the next lock is a group of
    // shared lock(s) or a single exclusive lock
    bool nextLockIsShared = false;

    deque<LockRequest>::iterator scan_it;
    for (scan_it = it + 1; scan_it != lock_requests->end() && scan_it->mode_ == SHARED; scan_it++) {
      // If this loop executes at least once, then we know that the next set of
      // locks is not exclusive
      nextLockIsShared = true;

      Txn* cur_txn = scan_it->txn_;
      txn_waits_[cur_txn]--;
      if  (txn_waits_[cur_txn] == 0) {
        // printf("Pushing back %p to ready txns\n", cur_txn);
        ready_txns_->push_back(cur_txn);
      }
    }

    //  If the next batch is a single exclusive lock request, then we need to decrement it, ONLY if it's at the front though.
    if (!nextLockIsShared && scan_it != lock_requests->end()) { 
      Txn* cur_txn = scan_it->txn_;
      txn_waits_[cur_txn]--;
      if  (txn_waits_[cur_txn] == 0) {
        // printf("Pushing back %p to ready txns\n", cur_txn);
        ready_txns_->push_back(cur_txn);
      }
    }
  }

  // Regardless of what we do, we need to erase the lock we intended to release
  lock_requests->erase(it);
}

LockMode LockManagerB::Status(const Key& key, vector<Txn*>* owners) {
  deque<LockRequest>*& lock_requests = lock_table_[key];

  if (lock_requests == 0 || lock_requests->size() == 0) {
    *owners = vector<Txn*>(0);
    return UNLOCKED;
  }

  *owners = vector<Txn*>(0);

  if (lock_requests->begin()->mode_ == EXCLUSIVE) {
    owners->push_back(lock_requests->begin()->txn_);
    return EXCLUSIVE;
  }

  deque<LockRequest>::iterator it;
  for (it = lock_requests->begin(); it != lock_requests->end() && it->mode_ != EXCLUSIVE; it++)
    owners->push_back(it->txn_);
  return SHARED;
}

