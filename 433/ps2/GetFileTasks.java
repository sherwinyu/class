// package com.sherwinyu.cs433.ps2;
package syu;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

import static syu.Utils.*;

public class GetFileTasks implements Runnable {
  protected String id;
  protected String server;
  protected int port;
  protected String[] filenames;
  protected int fileInd = 0;
  List<Integer> fileSizes;
  List<Integer> delays;
  static int counter = 0;

  long startTime;
  long endTime;
  DataOutputStream dataOutputStream;
  Socket socket;
  InetSocketAddress addr;

  public void run () {
    boolean timeup = true;
    while(timeup) {
      try {
        Thread.sleep(100);
        setUpConnection();
        nextRequest();
      }
      catch (InterruptedException e) {
        timeup = false;
        //reportStats(); TODO(syu) implement this
        // System.out.println("File sizes: " + Arrays.toString(this.fileSizes));
        // System.out.println("Delays: " + Arrays.toString(this.delays));
        // p(this, "File sizes: " + this.fileSizes);
        // p(this, "Delays: " + this.delays);


      }
      catch (IOException e) {
        // timeup = false;
        p("IOError occured. " + e.getMessage());
      }
    }
  }

  public void setUpConnection() throws IOException {
    p(this, "Opening connection: " + addr);
    this.socket = new Socket();
    this.socket.connect(this.addr);
    this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
  }

  public String requestFileMessage(String fn) {
    return "GET " + fn + " HTTP/1.0\r\n\r\n";
  }

  void nextRequest() throws IOException {
    p(this, "Getting filename: " + filenames[fileInd]);
    processFile(filenames[fileInd]);
    fileInd = (fileInd + 1) % filenames.length;
  }

  void processFile(String fn) throws IOException {
    String rfm = requestFileMessage(fn);
    writeMessage(rfm);
    long ts = System.currentTimeMillis();
    String resp = receiveResponse();
    long delay = System.currentTimeMillis() - ts;
    int size = resp.getBytes().length;
    p(this, "Received response. Delay: " + (int) delay + "\t Size: " + size +"\t Response: " + preview(resp));
    collectStats(size, (int) delay);
    //p(this, "Received response. Delay: " + (int) delay + "\t Size: " + size +"\t Response: " + resp);
    // p(this, "...Delay " + (int) delay);
    // p(this, "...size " + size);
  }

  void collectStats(int size, int delay)
  {
    if (delays !=null && fileSizes != null) {
      this.delays.add(delay);
      this.fileSizes.add(size);
    }
  }

  void writeMessage(String s) throws IOException {
    dataOutputStream.writeBytes(s);
  }

  String receiveResponse() throws IOException {
    InputStream is = socket.getInputStream();
    final char[] buffer = new char[0x10000];
    StringBuilder out = new StringBuilder();
    Reader in = new InputStreamReader(is);
    int read;
    do {
      read = in.read(buffer, 0, buffer.length);
      // p(this, 4, "read " + read + " bytes");
      if (read > 0) {
        out.append(buffer, 0, read);
      }
    } while (read >= 0);
    return out.toString();


    // return new Scanner(socket.getInputStream()).useDelimiter("\\A").next();
  }

  // time out is in seconds
  // public GetFileTasks(InetSocketAddress addr, String[] filenames, int timeout, List<Integer> fileSizes, List<Integer> delays) throws IOException {
  public GetFileTasks(InetSocketAddress addr, String[] filenames, int timeout, SHTTPTestClient tc) throws IOException {
    this.id = "GFT#" + GetFileTasks.counter++;
    this.addr = addr;
    this.filenames = filenames;
    this.dataOutputStream = null; 
    this.startTime = System.currentTimeMillis();
    this.endTime = startTime + timeout * 1000;
    this.fileSizes = tc.fileSizes;
    this.delays = tc.delays;
  }

  public GetFileTasks() {
  }

}

