
import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.*;

import static syu.Utils.*;
public class ThreadPoolCompetingServer extends Server {

  public static final String NAME = "ThreadPoolCompetingServer";
  public static final int NUM_DEFAULT_THREADS = 15;
  protected int numThreads;
  protected ExecutorService threadPool;

  public ThreadPoolCompetingServer() throws IOException {
    this(new ServerSocket(), NAME, ".");
  }

  public ThreadPoolCompetingServer(ServerSocket s, String serverName, String documentRoot) throws IOException {
    this(s, serverName, documentRoot, NUM_DEFAULT_THREADS);
  }

  public ThreadPoolCompetingServer(ServerSocket s, String serverName, String documentRoot, int numThreads) throws IOException {
    super(s, serverName, documentRoot);
    this.threadPool = Executors.newFixedThreadPool(numThreads);
  }

  @Override
    public void handleRequests() {

      for (int i = 0; i < numThreads; i++ )
        threadPool.execute(newThreadPoolCompetingRequestHandler());

      while(alive) {
        try {
          //this.wait(100);
          Thread.sleep(100);
        } catch (InterruptedException e) {
          alive = false;
          System.out.println("Interrupted -- shutting down server");
        }
      }
      // try {
        threadPool.shutdownNow();
    }

  public RequestHandler newThreadPoolCompetingRequestHandler() {
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


}

class ThreadPoolCompetingRequestHandler extends RequestHandler {

  private boolean alive;

  public ThreadPoolCompetingRequestHandler(Server s) {
    this.parentServer = s;
    this.connectionSocket = null;
  }


  @Override
    public void run() {
      connectionSocket = null;
      alive = true;
      while(alive) {
        try {
          synchronized(parentServer.listenSocket) {
            try {
              connectionSocket = parentServer.acceptIncomingConnection();
            } catch (IOException e) {
              System.out.println("IOException in " + this.id);
            }
          }
          handleRequest();
        }
        catch (IOException e) {
          this.alive = false;
          System.out.println("ThreadPoolCompetingRequestHandler id = " + id + ": shutting down due to IO error:\n");
          e.printStackTrace();
        }
      } // end while
    }// end run
}
