// package com.sherwinyu.cs433.ps2;

import java.io.*;
import java.net.*;
import java.util.*;

public class SequentialServer extends Server implements Runnable {

  RequestHandler rh;

  public static SequentialServer createFromArgs(String[] args) throws NumberFormatException, BindException, FileNotFoundException, IOException {

    SequentialServer ss = new SequentialServer();
    if (args.length !=2 || !args[0].equals("-config")) {
      throw new NumberFormatException();
    }
    BufferedReader br = new BufferedReader(new FileReader(args[1]));
    while(br.ready()) {
      String[] toks = br.readLine().split("\\s+");
      if (toks[0].equals("Listen"))
        ss.serverPort = Integer.parseInt(toks[1]);
      if (toks[0].equals("DocumentRoot"))
        ss.setDocumentRoot(toks[1]);
    }

    ss.listenSocket = new ServerSocket(ss.serverPort);
    System.out.println("server listening at: " + ss.serverPort);
    System.out.println("server www root: " + ss.documentRoot);

    return ss;
  }

  public SequentialServer(ServerSocket sock, String serverName, String documentRoot) {
  }

  public SequentialServer() {
  }

  public void handleRequests() throws IOException {

    Socket connectionSocket;
    while (alive) {
      connectionSocket = acceptIncomingConnection(); // blocking
      rh = getRequestHandler(connectionSocket);
      rh.handleRequest();
    }
  }

  public Socket acceptIncomingConnection() throws IOException {
    System.out.println("\nAccepting connection...");
    Socket socket =  listenSocket.accept();
    System.out.println("Accepted from" +socket.getLocalAddress() + ":" +socket.getPort() );
    return socket;
  }

  public RequestHandler getRequestHandler(Socket connectionSocket) {
    return new RequestHandler(this, connectionSocket);
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
