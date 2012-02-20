// package com.sherwinyu.cs433.ps2;

import java.io.*;
import java.net.*;
import java.util.*;

public class SequentialServer implements Runnable{

  public int serverPort = 6789;
  public String serverName = "SequentialServer";
  ServerSocket listenSocket;
  public boolean alive = true;

  //public static String WWW_ROOT = "/home/httpd/html/zoo/classes/cs433/";
  public String WWW_ROOT = "./";


  public static SequentialServer createFromArgs(String[] args) throws Exception {

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
    System.out.println("server www root: " + ss.WWW_ROOT);

    return ss;
  }

  public void setDocumentRoot(String dirname) throws IOException {
    if ((new File(dirname)).exists())
      this.WWW_ROOT = dirname;
    else
      throw new FileNotFoundException("Couldn't open document root: " + dirname);
  }

  public void run()
  {
    try {
      this.handleRequests();
    } catch (Exception e) {e.printStackTrace(); }
  }


  public void handleRequests() throws IOException {

    Socket connectionSocket;
    while (alive) {
      connectionSocket = acceptIncomingConnection(); // blocking
      System.out.println("hi --- verify");
      RequestHandler rh = new RequestHandler(connectionSocket, WWW_ROOT, serverName);
      rh.handleRequest();
    }
  }

  public Socket acceptIncomingConnection() throws IOException {
    System.out.println("\nAccepting connection...");
    Socket socket =  listenSocket.accept();
    System.out.println("Accepted from" +socket.getLocalAddress() + ":" +socket.getPort() );
    return socket;
  }



  public static void main(String args[]) throws Exception  {

    try {
      SequentialServer ss = createFromArgs(args);
      (new Thread(ss)).start();
    }
    catch (NumberFormatException e)
    {
      System.out.println("Usage: java SequentialServer -config <config_file>");
    }
    catch (FileNotFoundException e)
    {
      System.out.println(e.getMessage());
    }
    catch (BindException e)
    {
      System.out.println("Port unavailable");
    }


    // see if we do not use default server port


  } // end of main

} // end of class SimpleWebServer
