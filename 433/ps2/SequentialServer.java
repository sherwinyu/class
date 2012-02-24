// package com.sherwinyu.cs433.ps2;
package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;

public class SequentialServer extends SynchronousServer {
  static final String NAME = "SequentialServer";
  protected SyncRequestHandler rh;
  protected int counter = 0;

  public boolean isAcceptingNewConnections() {
    return true;
  }

  public SequentialServer(int port, String serverName, String documentRoot) throws IOException {
    super(new ServerSocket(port), serverName, documentRoot); //, FileCache.DEFAULTSIZE);
  }

  public SequentialServer(int port, String serverName, String documentRoot, int cacheSize) throws IOException {
    this(new ServerSocket(port), serverName, documentRoot, cacheSize);
  }

  public SequentialServer(ServerSocket sock, String serverName, String documentRoot, int cacheSize) throws IOException {
    super(sock, serverName, documentRoot, cacheSize);
    counter = 0;
    p(this, "cache=" + fileCache);
  }

  public SequentialServer() throws IOException {
    this(new ServerSocket(), NAME, ".", FileCache.DEFAULTSIZE);
  }


  @Override
    public void handleRequests() throws IOException {

      Socket connectionSocket;
      while (alive) {
        connectionSocket = acceptIncomingConnection(); // blocking
        rh = getRequestHandler(connectionSocket);
        rh.handleRequest();
      }
    }

  public SyncRequestHandler getRequestHandler(Socket connectionSocket) {
    SyncRequestHandler temp = new SyncRequestHandler(this, connectionSocket);
    temp.id = "" + counter++;
    return temp;
  }

  public static void main(String args[]) {

    try {
      HashMap<String, String> h = parseArgs(args);
      int port = Integer.parseInt(h.get("Listen"));
      String documentRoot = h.get("DocumentRoot");
      int cacheSize = FileCache.DEFAULTSIZE;
      if (h.get("CacheSize") != null)
        cacheSize = Integer.parseInt(h.get("CacheSize"));
      SequentialServer server = new SequentialServer(new ServerSocket(port), NAME, documentRoot, cacheSize);
      (new Thread(server)).start();
    }
    catch (NumberFormatException e) {
      System.out.println("Usage: java SequentialServer -config <config_file>");
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
