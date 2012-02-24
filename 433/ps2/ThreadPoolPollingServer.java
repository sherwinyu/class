package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.*;

import static syu.Utils.*;

public class ThreadPoolPollingServer extends ThreadPoolServer {

  protected int count = 0;
  public static final String NAME = "ThreadPoolPollingServer";
  protected List<Socket> socketQueue;

  public boolean isAcceptingNewConnections() {
    synchronized(socketQueue) {
      return socketQueue.size() < 20;
    }
  }

  public ThreadPoolPollingServer() throws IOException {
    this(new ServerSocket(), NAME, ".", ThreadPoolServer.NUM_DEFAULT_THREADS, FileCache.DEFAULTSIZE);
  }

  public ThreadPoolPollingServer(int numThreads) throws IOException {
    this(new ServerSocket(), NAME, ".", numThreads, FileCache.DEFAULTSIZE);
  }

  public ThreadPoolPollingServer(ServerSocket s, String serverName, String documentRoot, int numThreads, int cacheSize) throws IOException {
    super(s, serverName, documentRoot, numThreads, cacheSize);
    this.threadPool = Executors.newFixedThreadPool(this.numThreads);
    socketQueue = new ArrayList<Socket>();
  }

  @Override
    public void handleRequests() throws IOException {

      for (int i = 0; i < numThreads; i++ )
        threadPool.execute(newThreadPoolPollingRequestHandler());
      Socket incomingSocket;
      while (alive) {
        incomingSocket = acceptIncomingConnection();
        synchronized(socketQueue) {
          socketQueue.add(incomingSocket);
        }
      }
      threadPool.shutdownNow();

    }

  public ThreadPoolPollingRequestHandler newThreadPoolPollingRequestHandler() {
    ThreadPoolPollingRequestHandler tpprh =  new ThreadPoolPollingRequestHandler(this);
    tpprh.id = count++ + "";
    return tpprh;
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

class ThreadPoolPollingRequestHandler extends SyncRequestHandler {

  public ThreadPoolPollingRequestHandler(Server s) {
    this.parentServer = s;
    this.connectionSocket = null;
  }

  public List<Socket> getSocketQueue() {
    return ((ThreadPoolPollingServer) this.parentServer).socketQueue;
  }

  @Override
    public void run() {

      while (connectionSocket == null) {
        synchronized(getSocketQueue()) {
          if (!getSocketQueue().isEmpty()) {
            connectionSocket = getSocketQueue().remove(0);
            
          } // end synchronized
          if (connectionSocket != null )
            try {
              handleRequest();
            } catch (IOException e) {
              p(this,"ThreadPoolPollingRequestHandler id = " + id + ": encounterd error in handling request:\n");
            }
          connectionSocket = null;
        }
      }
    }

}
