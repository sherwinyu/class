package syu;

import java.nio.channels.*;
import java.io.IOException;
import java.util.*; 
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static syu.Utils.*;

public class AsyncServer extends Server {
  public static final String NAME = "AsyncServer";
  
  public static ServerSocketChannel serverChannel;

  protected Dispatcher dispatcher;

  public AsyncServer(int port, String serverName, String documentRoot) throws IOException //TODO(syu) -- what goes here?
  {
    super(serverName, documentRoot);
    this.serverChannel = openServerChannelAtPort(port);
    this.serverChannel.configureBlocking(false);
    this.dispatcher = new Dispatcher(this.serverChannel);
  }

  public ServerSocketChannel openServerChannelAtPort(int port) throws IOException {
    ServerSocketChannel ssc = ServerSocketChannel.open();
    ServerSocket ss = ssc.socket();
    ss.bind(new InetSocketAddress(port));
    p(this, "Server starting: " + ss);
    return ssc;
  }

  // ServerSocketChannel 


  public void handleRequests() {
    // 1. register the accept handler
    // 2. start the thread
    // 3. start the monitoring thread

    new Thread(dispatcher).start();
    //TODO(syu) monitoring thread? 
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
