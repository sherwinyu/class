// package com.sherwinyu.cs433.ps2;
package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;

public class SequentialServer extends Server {
  static final String NAME = "SequentialServer";
  protected RequestHandler rh;
  protected int counter = 0;

  public SequentialServer(int port, String serverName, String documentRoot) throws IOException {
    this(new ServerSocket(port), serverName, documentRoot);
  }

  public SequentialServer(ServerSocket sock, String serverName, String documentRoot) throws IOException {
    super(sock, serverName, documentRoot);
    counter = 0;
  }

  public SequentialServer() throws IOException {
    this(new ServerSocket(), NAME, ".");
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

  public RequestHandler getRequestHandler(Socket connectionSocket) {
    RequestHandler temp = new RequestHandler(this, connectionSocket);
    temp.id = "" + counter++;
    return temp;
  }

  public static void main(String args[]) {

    try {
      HashMap<String, String> h = parseArgs(args);
      int port = Integer.parseInt(h.get("Listen"));
      String documentRoot = h.get("DocumentRoot");
      SequentialServer server = new SequentialServer(new ServerSocket(port), NAME, documentRoot);
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
