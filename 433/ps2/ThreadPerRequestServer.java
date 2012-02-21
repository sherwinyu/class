// package com.sherwinyu.cs433.ps2;

import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;

public class ThreadPerRequestServer extends SequentialServer {

  static final String NAME = "ThreadPerRequestServer";
  int numThreadsStarted;

  public ThreadPerRequestServer(ServerSocket sock, String serverName, String documentRoot) throws IOException {
    super(sock, serverName, documentRoot);
    numThreadsStarted = 0;
  }

  public ThreadPerRequestServer() throws IOException {
    this(new ServerSocket(), NAME, ".");
  }

  @Override
  public void handleRequests() throws IOException {

    Socket connectionSocket;
    while (alive) {
      connectionSocket = acceptIncomingConnection(); // blocking
      RequestHandler rh = getRequestHandler(connectionSocket);
      startNewThread(rh);
    }
  }

  public void startNewThread(RequestHandler rh) {
      numThreadsStarted++;
      rh.id = "" + numThreadsStarted;
      (new Thread(rh)).start();
      System.out.println(" Number of thread Started: " + numThreadsStarted);//  + "id: " + rh.id);
  }


  public static void main(String args[]) {

    try {
      HashMap<String, String> h = parseArgs(args);
      int port = Integer.parseInt(h.get("Listen"));
      String documentRoot = h.get("DocumentRoot");
      ThreadPerRequestServer server = new ThreadPerRequestServer(new ServerSocket(port), NAME, documentRoot);
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
