
/*
 * Sherwin Yu
 * 2012 02 15
 * CS 433 PS 2
 */

/*
   Part 1: Simple Client

   Your test client should be multithreaded. The client can generate test requests to the server with the following command line:
   %java SHTTPTestClient -server <server> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>
   In particular, the <file name> is the name of a file that contains a list of files to request. For example, a file may look like the following:

   file1.html
   file2.html
   file3.html
   file1.html
   Then each thread of the client will request file1.html, then file2.html, then file3.html, and then file1.html. The thread then repeats the sequence. The client simply discards the received reply. The client stops after <time of test in seconds>. The client should print out the total transaction throughput (# files finished downloading by all threads, averaged over per second), data rate throughput (number bytes transmitted, averaged over per second), and the average of wait time (i.e., time from issuing request to getting first data). Think about how to collect statistics from multiple threads.
   */


import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

class GetFileTasks implements Runnable {
  protected String server;
  protected int port;
  protected int threadCount;
  protected String infile;

  long startTime;
  long endTime;
  DataOutputStream dataOutputStream;
  Socket clientSocket;

  public void run () {
    boolean timeup = true;

    while(timeup)
    {
      try {
        Thread.sleep(1000);
        getFile("filename");
      } catch (Exception e) {timeup = false; }
    }
  }

  public String requestFileMessage(String fn) {
    return fn;
  }

  void getFile(String fn) throws IOException {
    System.out.println("hashcode = " + this.hashCode() + "\tserver = " + server);
    writeMessage(fn);
  }

  void writeMessage(String s) throws IOException {
    dataOutputStream.writeBytes(s);
  }

  // time out is in seconds
  public GetFileTasks(Socket sock, String[] filenames, int timeout) throws IOException {
    this.server = server;
    clientSocket = sock; //new Socket(InetAddress.getByName(server), port);
    dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

    startTime = System.currentTimeMillis();
    endTime = startTime + timeout * 1000;
    // System.out.println("startime=" + startTime + "\tendtime= " +endTime);
  }

  public GetFileTasks(String server, int port, String[] filenames, int timeout) throws IOException {
    clientSocket = new Socket(InetAddress.getByName(server), port);
    dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

    startTime = System.currentTimeMillis();
    endTime = startTime + timeout * 1000;
    // System.out.println("startime=" + startTime + "\tendtime= " +endTime);
  }
  public GetFileTasks()
  {
  }
}

public class SHTTPTestClient {
  protected String server;
  protected int port;
  protected Socket clientSocket;

  protected int threadCount;
  protected String infile;
  protected String[] filenames;
  protected int timeout;
  protected ExecutorService executor;

  public SHTTPTestClient(String server, int port, int threadCount, String infile, int timeout) {
    this.server = server;
    this.port = port;
    this.threadCount = threadCount;
    this.infile = infile;
    this.timeout = timeout;
    try {
    this.clientSocket = new Socket(InetAddress.getByName(server), port);
    } catch (Exception e) {e.printStackTrace();}
    this.executor = new ScheduledThreadPoolExecutor(this.threadCount);
  }

  public SHTTPTestClient(Socket sock, int threadCount, String infile, int timeout) {
    this.threadCount = threadCount;
    this.infile = infile;
    this.timeout = timeout;
    this.clientSocket = sock;
    this.executor = new ScheduledThreadPoolExecutor(this.threadCount);
  }

  public Socket getSocket(String s, int p) throws UnknownHostException, IOException  {
    return new Socket(InetAddress.getByName(s), p);
  }

  public GetFileTasks createGetFileTask(Socket s, String[] filenames, int timeout) throws UnknownHostException, IOException  {
    return new GetFileTasks(s, filenames, timeout);
  }

  public void start() throws Exception {
    for (int i = 0; i< threadCount; i++)
      // executor.execute(new GetFileTasks((Socket) clientSocket.clone(), filenames, timeout));
      // executor.execute(new GetFileTasks(new Socket(InetAddress.getByName(server), port), filenames, timeout));
      // executor.execute(new GetFileTasks(getSocket(server, port), filenames, timeout));
      executor.execute(createGetFileTask(getSocket(server, port), filenames, timeout));
    Thread.sleep(timeout * 1000);
    executor.shutdownNow();
    System.out.println("End start");
  }

  // %java SHTTPTestClient -server <server> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>
  public static void main(String[] args) {
    try {
      SHTTPTestClient stc = createFromArgs(args);
      stc.start();
    } catch (Exception e)
    {
      System.out.println("Usage: java SHTTPTestClient -server <server> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>");
    }

  }

  public static SHTTPTestClient createFromArgs(String[] args) throws NumberFormatException, ArrayIndexOutOfBoundsException {
    String server = "";
    int port = 0;
    int threadCount = 0;
    String infile = "";
    int timeout = 5000;
    if (args.length != 10) throw new NumberFormatException();

    for(int i = 0; i < args.length; i++) { // need +1 because we increment by two
      if (args[i].equals("-server")) {
        server = args[i+1];
        i++;
        continue;
      }
      if (args[i].equals("-port")) {
        port = Integer.parseInt(args[i+1]);
        i++;
        continue;
      }
      if (args[i].equals("-parallel")) {
        threadCount = Integer.parseInt(args[i+1]);
        i++;
        continue;
      }
      if (args[i].equals("-files")) {
        infile = args[i+1];
        i++;
        continue;
      }
      if (args[i].equals("-T")) {
        timeout = Integer.parseInt(args[i+1]);
        continue;
      }
    }
    SHTTPTestClient stc = new SHTTPTestClient(server, port, threadCount, infile, timeout);

    return stc;

  }
}
