package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;

public class ThreadPerRequestServer extends SequentialServer {

  static final String NAME = "ThreadPerRequestServer";
  int numThreadsStarted;

  public ThreadPerRequestServer(ServerSocket sock, String serverName, String documentRoot, int cacheSize) throws IOException {
    super(sock, serverName, documentRoot, cacheSize);
    numThreadsStarted = 0;
  }

  public ThreadPerRequestServer() throws IOException {
    this(new ServerSocket(), NAME, ".", FileCache.DEFAULTSIZE);
  }

  @Override
  public void handleRequests() throws IOException {

    Socket connectionSocket;
    while (alive) {
      connectionSocket = acceptIncomingConnection(); // blocking
      SyncRequestHandler rh = getRequestHandler(connectionSocket);
      startNewThread(rh);
    }
  }

  public void startNewThread(SyncRequestHandler rh) {
      numThreadsStarted++;
      rh.id = "" + numThreadsStarted;
      (new Thread(rh)).start();
      p(this," Number of thread Started: " + numThreadsStarted);//  + "id: " + rh.id);
  }


  public static void main(String args[]) {

    try {
      HashMap<String, String> h = parseArgs(args);
      int port = Integer.parseInt(h.get("Listen"));
      String documentRoot = h.get("DocumentRoot");
      int cacheSize = FileCache.DEFAULTSIZE;
      if (h.get("CacheSize") != null)
        cacheSize = Integer.parseInt(h.get("CacheSize"));
      ThreadPerRequestServer server = new ThreadPerRequestServer(new ServerSocket(port), NAME, documentRoot, cacheSize);
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

} // end of class SimpleWebServer
