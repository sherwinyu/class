
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


package syu;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

public class SHTTPTestClient {

  protected String server;
  protected int port;
  protected InetSocketAddress addr;

  protected int threadCount;
  protected String infile;
  protected String[] filenames;
  protected int timeout;
  protected ExecutorService executor;

  public SHTTPTestClient(String server, int port, int threadCount, String infile, int timeout) throws UnknownHostException {
    this.server = server;
    this.port = port;
    InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(server), port);
    this.addr = isa;
    this.threadCount = threadCount;
    this.infile = infile;
    this.timeout = timeout;
    this.executor = new ScheduledThreadPoolExecutor(this.threadCount);
  }

  public SHTTPTestClient(InetSocketAddress addr, int threadCount, String infile, int timeout) throws UnknownHostException {
    this(addr.getHostName(), addr.getPort(), threadCount, infile, timeout);
  }

  /* Factory method for testing */
  public GetFileTasks createGetFileTask(InetSocketAddress s, String[] filenames, int timeout) throws UnknownHostException, IOException  {
    return new GetFileTasks(s, filenames, timeout);
  }

  public void start()  {
    System.out.println("Beginning to send requests to " + addr);
    try{
      for (int i = 0; i< threadCount; i++)
        executor.execute(createGetFileTask(addr, filenames, timeout)); //TODO(syu): pass in data structure for collecint stat summary; remove factory method
      Thread.sleep(timeout * 1000);
      executor.shutdownNow();
      System.out.println("Terminating.");
    }
    catch (IOException e) { e.printStackTrace(); }
    catch (InterruptedException e) { e.printStackTrace(); }

  }

  // %java SHTTPTestClient -server <server> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>
  public static void main(String[] args) {
    SHTTPTestClient stc = null;
    try {
      stc = createFromArgs(args);
    } catch (NumberFormatException e)
    {
      System.out.println("Usage: java SHTTPTestClient -server <server> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>");
    } catch (ArrayIndexOutOfBoundsException e)
    {
      System.out.println("Usage: java SHTTPTestClient -server <server> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>");
    }
    catch (IOException e)
    {
      System.out.println("Couldn't open file" + e.getMessage());
    }
    if (stc != null)
      stc.start();


  }

  public static SHTTPTestClient createFromArgs(String[] args) throws NumberFormatException, ArrayIndexOutOfBoundsException, IOException {
    System.out.println("args: " + Arrays.toString(args));
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
        System.out.println("timeout: " + timeout);
        continue;
      }
    }
    SHTTPTestClient stc = new SHTTPTestClient(server, port, threadCount, infile, timeout);

    ArrayList<String> files = new ArrayList<String>();
    BufferedReader br = new BufferedReader(new FileReader(infile));
    while(br.ready())
    {
      String s = br.readLine();
      files.add(s);
      System.out.println(s);
    }
    stc.filenames = files.toArray(new String[]{});

    return stc;
  }
}
