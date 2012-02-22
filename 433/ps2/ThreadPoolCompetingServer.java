package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.*;

import static syu.Utils.*;
public class ThreadPoolCompetingServer extends ThreadPoolServer {

  public static final String NAME = "ThreadPoolCompetingServer";

  public ThreadPoolCompetingServer() throws IOException {
    this(new ServerSocket(), NAME, ".", ThreadPoolServer.NUM_DEFAULT_THREADS);
  }

  public ThreadPoolCompetingServer(int numThreads) throws IOException {
    this(new ServerSocket(), NAME, ".", numThreads);
  }

  public ThreadPoolCompetingServer(ServerSocket s, String serverName, String documentRoot, int numThreads) throws IOException {
    super(s, serverName, documentRoot, numThreads);
    this.threadPool = Executors.newFixedThreadPool(this.numThreads);
  }

  @Override
    public void handleRequests() {

      for (int i = 0; i < numThreads; i++ )
        threadPool.execute(newThreadPoolCompetingRequestHandler());

      while(alive) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          alive = false;
          p(this,"Interrupted -- shutting down server");
        }
      }
        threadPool.shutdownNow();
    }

  public ThreadPoolCompetingRequestHandler newThreadPoolCompetingRequestHandler() {
    return new ThreadPoolCompetingRequestHandler(this);
  }

  public static void main (String[] args) {
    try {
      HashMap<String, String> h = parseArgs(args);
      int port = Integer.parseInt(h.get("Listen"));
      String documentRoot = h.get("DocumentRoot");
      int threadPoolSize = Integer.parseInt(h.get("ThreadPoolSize"));
      ThreadPoolCompetingServer server = new ThreadPoolCompetingServer(new ServerSocket(port), NAME, documentRoot, threadPoolSize);
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

class ThreadPoolCompetingRequestHandler extends RequestHandler {

  private boolean alive;

  public ThreadPoolCompetingRequestHandler(Server s) {
    this.parentServer = s;
    this.connectionSocket = null;
  }

  public ThreadPoolCompetingServer getParentServer()   {
    return (ThreadPoolCompetingServer) this.parentServer;
  }

  @Override
    public void run() {
      connectionSocket = null;
      alive = true;
      while(alive) {
        try {
          synchronized(getParentServer().listenSocket) {
            try {
              connectionSocket = getParentServer().acceptIncomingConnection();
            } catch (IOException e) {
              p(this,"IOException in " + this.id);
            }
          }
          handleRequest();
        }
        catch (IOException e) {
          this.alive = false;
          p(this,"ThreadPoolCompetingRequestHandler id = " + id + ": shutting down due to IO error:\n");
          e.printStackTrace();
        }
      } // end while
    }// end run
}
