package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.*;

import static syu.Utils.*;

public abstract class ThreadPoolServer extends SynchronousServer {
  public static final int NUM_DEFAULT_THREADS = 15;
  protected int numThreads;
  protected ExecutorService threadPool;
  protected int count = 0;

  public ThreadPoolServer(ServerSocket s, String serverName, String documentRoot) throws IOException {
    super(s, serverName, documentRoot);
    this.numThreads = NUM_DEFAULT_THREADS;
  }

  public ThreadPoolServer(ServerSocket s, String serverName, String documentRoot, int numThreads, int cacheSize) throws IOException {
    super(s, serverName, documentRoot);
    this.numThreads = numThreads;
    p(this, 1, "with " + numThreads + "threads");
  }
}
