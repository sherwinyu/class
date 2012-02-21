// package com.sherwinyu.cs433.ps2;

import java.io.*;
import java.net.*;
import java.util.*;

public class SequentialServer extends Server {
  static final String NAME = "SequentialServer";
  protected RequestHandler rh;
  protected int counter = 0;

  public static SequentialServer createFromArgs(String[] args) throws NumberFormatException, BindException, FileNotFoundException, IOException {

    // SequentialServer ss = new SequentialServer();
    int port = -1;
    String docroot = "";

    if (args.length !=2 || !args[0].equals("-config")) {
      throw new NumberFormatException();
    }
    BufferedReader br = new BufferedReader(new FileReader(args[1]));
    while(br.ready()) {
      String[] toks = br.readLine().split("\\s+");
      if (toks[0].equals("Listen"))
        port  = Integer.parseInt(toks[1]);
      if (toks[0].equals("DocumentRoot"))
        docroot = toks[1];
    }

    return new SequentialServer(new ServerSocket(port), NAME, ".");
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
      SequentialServer ss = createFromArgs(args);
      (new Thread(ss)).start();
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
