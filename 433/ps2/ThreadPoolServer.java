package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.*;

import static syu.Utils.*;

public abstract class ThreadPoolServer extends Server {
  public static final int NUM_DEFAULT_THREADS = 15;
  protected int numThreads;
  protected ExecutorService threadPool;

  public ThreadPoolServer(ServerSocket s, String serverName, String documentRoot) throws IOException {
    super(s, serverName, documentRoot);
  }

  public ThreadPoolServer(ServerSocket s, String serverName, String documentRoot, int numThreads) throws IOException {
    super(s, serverName, documentRoot);
    this.numThreads = numThreads;
  }
}
