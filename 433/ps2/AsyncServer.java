package syu;

import java.nio.channels.*;
import java.io.IOException;
import java.util.*; 
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static syu.Utils.*;

public class AsyncServer implements Runnable {
  public static final String NAME = "AsyncServer";
  public static ServerSocketChannel serverChannel;

  protected Dispatcher dispatcher;

  public AsyncServer(int port, String serverName, String documentRoot) //TODO(syu) -- what goes here?
  {
    dispatcher = new Dispatcher();


  }

  public void run() {
    handleRequests();
  }

  public void handleRequests() {
    new Thread(dispatcher).start();
  }

  public static void main(String[] args) {
    try {
      HashMap<String, String> h = parseArgs(args);
      int port = Integer.parseInt(h.get("Listen"));
      String documentRoot = h.get("DocumentRoot");
      AsyncServer server = new AsyncServer(port, NAME, documentRoot) ;
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
