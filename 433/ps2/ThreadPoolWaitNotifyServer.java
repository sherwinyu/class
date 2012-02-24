package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.*;

import static syu.Utils.*;

public class ThreadPoolWaitNotifyServer extends ThreadPoolServer {

  protected int count = 0;
  public static final String NAME = "ThreadPoolWaitNotifyServer";
  protected List<Socket> socketQueue;

  public boolean isAcceptingNewConnections() {
    synchronized(socketQueue) {
      return socketQueue.size() < 20;
    }
  }
  public ThreadPoolWaitNotifyServer() throws IOException {
    this(new ServerSocket(), NAME, ".", ThreadPoolServer.NUM_DEFAULT_THREADS, FileCache.DEFAULTSIZE);
  }

  public ThreadPoolWaitNotifyServer(int numThreads) throws IOException {
    this(new ServerSocket(), NAME, ".", numThreads, FileCache.DEFAULTSIZE);
  }

  public ThreadPoolWaitNotifyServer(ServerSocket s, String serverName, String documentRoot, int numThreads, int cacheSize) throws IOException {
    super(s, serverName, documentRoot, numThreads, cacheSize);
    this.threadPool = Executors.newFixedThreadPool(this.numThreads);
    socketQueue = new ArrayList<Socket>();
  }

  @Override
    public void handleRequests() throws IOException {

      for (int i = 0; i < numThreads; i++ )
        threadPool.execute(newThreadPoolWaitNotifyRequestHandler());
      Socket incomingSocket;

      while (alive) {
        incomingSocket = acceptIncomingConnection();
        synchronized(socketQueue) {
          socketQueue.add(incomingSocket);
          socketQueue.notifyAll();
        }
      }
      threadPool.shutdownNow();

    }


  public ThreadPoolWaitNotifyRequestHandler newThreadPoolWaitNotifyRequestHandler() {
    ThreadPoolWaitNotifyRequestHandler tpwnrh =  new ThreadPoolWaitNotifyRequestHandler(this);
    tpwnrh.id = "Thread#"+count++;
    return tpwnrh;
  }


  public static void main (String[] args) {
    try {
      HashMap<String, String> h = parseArgs(args);
      int port = Integer.parseInt(h.get("Listen"));
      String documentRoot = h.get("DocumentRoot");
      int threadPoolSize = Integer.parseInt(h.get("ThreadPoolSize"));
      int cacheSize = FileCache.DEFAULTSIZE;
      if (h.get("CacheSize") != null)
        cacheSize = Integer.parseInt(h.get("CacheSize"));
      ThreadPoolPollingServer server = new ThreadPoolPollingServer(new ServerSocket(port), NAME, documentRoot, threadPoolSize, cacheSize);
      (new Thread(server)).start();
    }
    catch (NumberFormatException e) {
      System.out.println("Usage: java " + NAME + " -config <config_file>");
    }
    catch (FileNotFoundException e) {
      System.out.println(e.getMessage());
    }
    catch (BindException e) {
      System.out.println("Port unavailable");
    }
    catch (IOException e) {
      System.out.println("IO Error. " + e.getMessage());
    }
  } // end of main

}

class ThreadPoolWaitNotifyRequestHandler extends SyncRequestHandler {

  public ThreadPoolWaitNotifyRequestHandler(Server s) {
    this.parentServer = s;
    this.connectionSocket = null;
  }

  public List<Socket> getSocketQueue() {
    return ((ThreadPoolWaitNotifyServer) this.parentServer).socketQueue;
  }

  @Override
    public void run() {

      while (true) {
        synchronized(getSocketQueue()) {
          while (getSocketQueue().isEmpty()) { // in CS
            p(this, "waiting");
            try {
              getSocketQueue().wait(); // relinquish lock and wait
            } catch (InterruptedException e) {
              p(this, "waiting interrupted");
            }
          }
          // condition: socketQueue is non empty and we are in CS
          connectionSocket = getSocketQueue().remove(0);
          p(this, "acquired connection: " + connectionSocket);
        } // have the socket, no longer need to be in CS
        try {
          handleRequest();
        } catch (IOException e) {
          p(this,"encounterd error in handling request");
        }
      }
    }
}
