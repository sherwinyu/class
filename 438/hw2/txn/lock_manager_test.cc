// Author: Alexander Thomson (thomson@cs.yale.edu)

#include "txn/lock_manager.h"

#include <set>
#include <string>

#include "utils/testing.h"
#include "txn/txn_types.h"
#include <iostream>

using namespace std;
using std::set;

TEST(LockManagerA_TestWriteLock) {

  deque<Txn*> ready_txns;
  LockManagerA lm(&ready_txns);
  vector<Txn*> owners;

  Txn* t1 = reinterpret_cast<Txn*>(new Noop());
  Txn* t2 = reinterpret_cast<Txn*>(new Noop());
  Txn* t3 = reinterpret_cast<Txn*>(new Noop());

  EXPECT_EQ(true, lm.WriteLock(t1, 101));
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[101])[0].mode_);
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(t1, (*lm.lock_table_[101])[0].txn_);
  EXPECT_EQ(1, (*lm.lock_table_[101]).size());

  EXPECT_EQ(false, lm.WriteLock(t2, 101));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(1, lm.txn_waits_[t2]);
  EXPECT_EQ(2, (*lm.lock_table_[101]).size());
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[101])[1].mode_);

  EXPECT_EQ(true, lm.WriteLock(t1, 555));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(1, lm.txn_waits_[t2]);
  EXPECT_EQ(2, (*lm.lock_table_[101]).size());
  EXPECT_EQ(2, (*lm.lock_table_[101]).size());
  EXPECT_EQ(1, (*lm.lock_table_[555]).size());
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[555]).back().mode_);
  EXPECT_EQ(t1, (*lm.lock_table_[555]).back().txn_);

  EXPECT_EQ(false, lm.WriteLock(t3, 101));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(1, lm.txn_waits_[t2]);
  EXPECT_EQ(1, lm.txn_waits_[t3]);
  EXPECT_EQ(t3, (*lm.lock_table_[101]).back().txn_);
  EXPECT_EQ(3, (*lm.lock_table_[101]).size());

  EXPECT_EQ(false, lm.WriteLock(t2, 555));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(2, lm.txn_waits_[t2]);
  EXPECT_EQ(1, lm.txn_waits_[t3]);
  EXPECT_EQ(2, (*lm.lock_table_[555]).size());
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[555]).back().mode_);
  EXPECT_EQ(t2, (*lm.lock_table_[555]).back().txn_);

  END;
}

TEST(LockManagerA_TestReleaseLock) {
  
  printf("\n\n\n\n\n");
  deque<Txn*> ready_txns = deque<Txn*>(0);
  EXPECT_EQ(0, ready_txns.size());

  LockManagerA lm(&ready_txns);
  vector<Txn*> owners;

  Txn* t1 = reinterpret_cast<Txn*>(new Noop());
  Txn* t2 = reinterpret_cast<Txn*>(new Noop());
  Txn* t3 = reinterpret_cast<Txn*>(new Noop());

  lm.WriteLock(t1, 101);
  lm.WriteLock(t2, 101);
  lm.WriteLock(t3, 101);
  EXPECT_EQ(3, (*lm.lock_table_[101]).size());

  // lock_requests: t1 t2 t3
  lm.Release(t1, 101);

  // should have added t2 to ready_txns because it's up next
  EXPECT_EQ(1, ready_txns.size());
  EXPECT_EQ(t2, ready_txns.back());

  // lock_requests: t2 t3
  EXPECT_EQ(2, (*lm.lock_table_[101]).size());
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[101]).at(0).mode_);
  EXPECT_EQ(t2, (*lm.lock_table_[101]).at(0).txn_);
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[101]).back().mode_);
  EXPECT_EQ(t3, (*lm.lock_table_[101]).back().txn_);

  lm.WriteLock(t1, 101);
  // lock requests: should be t2 t3 t1
  lm.Release(t3, 101); 

  // shouldn't have changed ready_txns
  EXPECT_EQ(1, ready_txns.size());
  EXPECT_EQ(t2, ready_txns.back());

  // lock requests: should be t2 t1
  EXPECT_EQ(2, lm.lock_table_[101]->size());
  EXPECT_EQ(t2, (*lm.lock_table_[101]).at(0).txn_);
  EXPECT_EQ(t1, (*lm.lock_table_[101]).at(1).txn_);

  lm.Release(t2, 101); 
  EXPECT_EQ(1, lm.lock_table_[101]->size());
  EXPECT_EQ(t1, (*lm.lock_table_[101]).at(0).txn_);
  EXPECT_EQ(1, lm.lock_table_[101]->size());

  lm.Release(t1, 101); 
  EXPECT_EQ(0, lm.lock_table_[101]->size());
  EXPECT_EQ(0, lm.lock_table_[101]->size());

  EXPECT_EQ(2, ready_txns.size());
  EXPECT_EQ(t1, ready_txns.back());

  EXPECT_EQ(true, lm.WriteLock(t1, 101));



  END;
  printf("\n\n\n\n\n");

}


TEST(LockManagerA_SimpleLocking) {
  deque<Txn*> ready_txns;
  LockManagerA lm(&ready_txns);
  vector<Txn*> owners;

  Txn* t1 = reinterpret_cast<Txn*>(1);
  Txn* t2 = reinterpret_cast<Txn*>(2);
  Txn* t3 = reinterpret_cast<Txn*>(3);

  // Txn 1 acquires read lock.
  lm.ReadLock(t1, 101);
  ready_txns.push_back(t1);   // Txn 1 is ready.
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t1, owners[0]);
  EXPECT_EQ(1, ready_txns.size());
  EXPECT_EQ(t1, ready_txns.at(0));

  // Txn 2 requests write lock. Not granted.
  lm.WriteLock(t2, 101);
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t1, owners[0]);
  EXPECT_EQ(1, ready_txns.size());

  // Txn 3 requests read lock. Not granted.
  lm.ReadLock(t3, 101);
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t1, owners[0]);
  EXPECT_EQ(1, ready_txns.size());

  // Txn 1 releases lock.  Txn 2 is granted write lock.
  lm.Release(t1, 101);
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t2, owners[0]);
  EXPECT_EQ(2, ready_txns.size());
  EXPECT_EQ(t2, ready_txns.at(1));

  // Txn 2 releases lock.  Txn 3 is granted read lock.
  lm.Release(t2, 101);
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t3, owners[0]);
  EXPECT_EQ(3, ready_txns.size());
  EXPECT_EQ(t3, ready_txns.at(2));

  END;
}

TEST(LockManagerA_LocksReleasedOutOfOrder) {
  deque<Txn*> ready_txns;
  LockManagerA lm(&ready_txns);
  vector<Txn*> owners;

  Txn* t1 = reinterpret_cast<Txn*>(1);
  Txn* t2 = reinterpret_cast<Txn*>(2);
  Txn* t3 = reinterpret_cast<Txn*>(3);
  Txn* t4 = reinterpret_cast<Txn*>(4);

  lm.ReadLock(t1, 101);   // Txn 1 acquires read lock.
  ready_txns.push_back(t1);  // Txn 1 is ready.
  lm.WriteLock(t2, 101);  // Txn 2 requests write lock. Not granted.
  lm.ReadLock(t3, 101);   // Txn 3 requests read lock. Not granted.
  lm.ReadLock(t4, 101);   // Txn 4 requests read lock. Not granted.

  lm.Release(t2, 101);    // Txn 2 cancels write lock request.

  // Txn 1 should now have a read lock and Txns 3 and 4 should be next in line.
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t1, owners[0]);

  // Txn 1 releases lock.  Txn 2 is granted read lock.
  lm.Release(t1, 101);
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t3, owners[0]);
  EXPECT_EQ(2, ready_txns.size());
  EXPECT_EQ(t3, ready_txns.at(1));

  // Txn 3 releases lock.  Txn 4 is granted read lock.
  lm.Release(t3, 101);
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t4, owners[0]);
  EXPECT_EQ(3, ready_txns.size());
  EXPECT_EQ(t4, ready_txns.at(2));

  END;
}

TEST(LockManagerB_TestReadLock) {
  deque<Txn*> ready_txns;
  LockManagerB lm(&ready_txns);

  Txn* t1 = reinterpret_cast<Txn*>(new Noop());
  Txn* t2 = reinterpret_cast<Txn*>(new Noop());
  Txn* t3 = reinterpret_cast<Txn*>(new Noop());
  Txn* t4 = reinterpret_cast<Txn*>(new Noop());
  Txn* t5 = reinterpret_cast<Txn*>(new Noop());

  // ReadLocks return true if there are only shared locks
  EXPECT_EQ(true, lm.ReadLock(t1, 101));
  EXPECT_EQ(true, lm.ReadLock(t2, 101));
  EXPECT_EQ(true, lm.ReadLock(t3, 101));
  EXPECT_EQ(false, lm.WriteLock(t4, 101));
  EXPECT_EQ(false, lm.ReadLock(t5, 101));

  END;
}

TEST(LockManagerB_TestWriteLock) {
  deque<Txn*> ready_txns;
  LockManagerB lm(&ready_txns);

  Txn* t1 = reinterpret_cast<Txn*>(new Noop());
  Txn* t2 = reinterpret_cast<Txn*>(new Noop());
  Txn* t3 = reinterpret_cast<Txn*>(new Noop());

  EXPECT_EQ(true, lm.WriteLock(t1, 101));
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[101])[0].mode_);
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(t1, (*lm.lock_table_[101])[0].txn_);
  EXPECT_EQ(1, (*lm.lock_table_[101]).size());

  EXPECT_EQ(false, lm.WriteLock(t2, 101));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(1, lm.txn_waits_[t2]);
  EXPECT_EQ(2, (*lm.lock_table_[101]).size());
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[101])[1].mode_);

  EXPECT_EQ(true, lm.WriteLock(t1, 555));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(1, lm.txn_waits_[t2]);
  EXPECT_EQ(2, (*lm.lock_table_[101]).size());
  EXPECT_EQ(2, (*lm.lock_table_[101]).size());
  EXPECT_EQ(1, (*lm.lock_table_[555]).size());
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[555]).back().mode_);
  EXPECT_EQ(t1, (*lm.lock_table_[555]).back().txn_);

  EXPECT_EQ(false, lm.WriteLock(t3, 101));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(1, lm.txn_waits_[t2]);
  EXPECT_EQ(1, lm.txn_waits_[t3]);
  EXPECT_EQ(t3, (*lm.lock_table_[101]).back().txn_);
  EXPECT_EQ(3, (*lm.lock_table_[101]).size());

  EXPECT_EQ(false, lm.WriteLock(t2, 555));
  EXPECT_EQ(0, lm.txn_waits_[t1]);
  EXPECT_EQ(2, lm.txn_waits_[t2]);
  EXPECT_EQ(1, lm.txn_waits_[t3]);
  EXPECT_EQ(2, (*lm.lock_table_[555]).size());
  EXPECT_EQ(EXCLUSIVE, (*lm.lock_table_[555]).back().mode_);
  EXPECT_EQ(t2, (*lm.lock_table_[555]).back().txn_);

  END;
}

TEST(LockManagerB_TestStatus) {

  END;
}

TEST(LockManagerB_TestRelease) {
  END;
}



TEST(LockManagerB_SimpleLocking) {
  deque<Txn*> ready_txns;
  LockManagerB lm(&ready_txns);
  vector<Txn*> owners;

  Txn* t1 = reinterpret_cast<Txn*>(1);
  Txn* t2 = reinterpret_cast<Txn*>(2);
  Txn* t3 = reinterpret_cast<Txn*>(3);

  // Txn 1 acquires read lock.
  lm.ReadLock(t1, 101);
  ready_txns.push_back(t1);   // Txn 1 is ready.
  EXPECT_EQ(SHARED, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t1, owners[0]);
  EXPECT_EQ(1, ready_txns.size());
  EXPECT_EQ(t1, ready_txns.at(0));

  // Txn 2 requests write lock. Not granted.
  lm.WriteLock(t2, 101);
  EXPECT_EQ(SHARED, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t1, owners[0]);
  EXPECT_EQ(1, ready_txns.size());

  // Txn 3 requests read lock. Not granted.
  lm.ReadLock(t3, 101);
  EXPECT_EQ(SHARED, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t1, owners[0]);
  EXPECT_EQ(1, ready_txns.size());

  // Txn 1 releases lock.  Txn 2 is granted write lock.
  lm.Release(t1, 101);
  EXPECT_EQ(EXCLUSIVE, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t2, owners[0]);
  EXPECT_EQ(2, ready_txns.size());
  EXPECT_EQ(t2, ready_txns.at(1));

  // Txn 2 releases lock.  Txn 3 is granted read lock.
  lm.Release(t2, 101);
  EXPECT_EQ(SHARED, lm.Status(101, &owners));
  EXPECT_EQ(1, owners.size());
  EXPECT_EQ(t3, owners[0]);
  EXPECT_EQ(3, ready_txns.size());
  EXPECT_EQ(t3, ready_txns.at(2));

  END;
}

TEST(LockManagerB_LocksReleasedOutOfOrder) {
  deque<Txn*> ready_txns;
  LockManagerB lm(&ready_txns);
  vector<Txn*> owners;

  Txn* t1 = reinterpret_cast<Txn*>(1);
  Txn* t2 = reinterpret_cast<Txn*>(2);
  Txn* t3 = reinterpret_cast<Txn*>(3);
  Txn* t4 = reinterpret_cast<Txn*>(4);

  lm.ReadLock(t1, 101);   // Txn 1 acquires read lock.
  ready_txns.push_back(t1);  // Txn 1 is ready.
  lm.WriteLock(t2, 101);  // Txn 2 requests write lock. Not granted.
  lm.ReadLock(t3, 101);   // Txn 3 requests read lock. Not granted.
  lm.ReadLock(t4, 101);   // Txn 4 requests read lock. Not granted.

  lm.Release(t2, 101);    // Txn 2 cancels write lock request.

  // Txns 1, 3 and 4 should now have a shared lock.
  EXPECT_EQ(SHARED, lm.Status(101, &owners));
  EXPECT_EQ(3, owners.size());
  EXPECT_EQ(t1, owners[0]);
  EXPECT_EQ(t3, owners[1]);
  EXPECT_EQ(t4, owners[2]);
  EXPECT_EQ(3, ready_txns.size());
  EXPECT_EQ(t1, ready_txns.at(0));
  EXPECT_EQ(t3, ready_txns.at(1));
  EXPECT_EQ(t4, ready_txns.at(2));

  END;
}

int main(int argc, char** argv) {
  LockManagerA_TestWriteLock();
  LockManagerA_TestReleaseLock();
  LockManagerA_SimpleLocking();
  LockManagerA_LocksReleasedOutOfOrder();
  LockManagerB_TestWriteLock();
  LockManagerB_TestReadLock();
  // LockManagerB_SimpleLocking();
  // LockManagerB_LocksReleasedOutOfOrder();
}

