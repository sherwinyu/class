// package com.sherwinyu.cs433.ps2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

import static syu.Utils.*;

public class GetFileTasks implements Runnable {
  protected String server;
  protected int port;
  protected String[] filenames;
  protected int fileInd = 0;
  ArrayList<Integer> fileSizes = new ArrayList<Integer>();
  ArrayList<Integer> delays = new ArrayList<Integer>();

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
        System.out.println("File sizes: " + this.fileSizes);
        System.out.println("Delays: " + this.delays);


      }
      catch (IOException e) {
        timeup = false;
        System.out.println("IOError occured. " + e.getMessage());
      }
    }
  }

  public void setUpConnection() throws IOException {
    System.out.print("Setting up connection" + addr);
      this.socket = new Socket();
      this.socket.connect(this.addr);
      this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());

    System.out.println("...done setting up connection with ..." + this.socket);
  }

  public String requestFileMessage(String fn) {
    return "GET " + fn + " HTTP/1.0\r\n\r\n";
  }

  void nextRequest() throws IOException {
    System.out.println("thread#" + this.hashCode() + ":\tpreparing to get " + filenames[fileInd]);
    processFile(filenames[fileInd]);
    fileInd = (fileInd + 1) % filenames.length;
  }

  void processFile(String fn) throws IOException {
    String rfm = requestFileMessage(fn);
    System.out.println("...preparing to write message:" + inspect(rfm));
    writeMessage(rfm);
    long ts = System.currentTimeMillis();
    String resp = receiveResponse();

    long delay = System.currentTimeMillis() - ts;
    int size = resp.getBytes().length;
    collectStats(size, (int) delay);
    System.out.println("...Received response: " + resp);
    System.out.println("...Delay " + (int) delay);
    System.out.println("...size " + size);
  }

  void collectStats(int size, int delay)
  {
    this.delays.add( delay);
    this.fileSizes.add(size);
  }

  void writeMessage(String s) throws IOException {
    dataOutputStream.writeBytes(s);
  }

  String receiveResponse() throws IOException {

    p("...receiving Response");
    return new Scanner(socket.getInputStream()).useDelimiter("\\A").next();
  }

  // time out is in seconds
  public GetFileTasks(InetSocketAddress addr, String[] filenames, int timeout) throws IOException {
    this.addr = addr;
    this.filenames = filenames;
    this.dataOutputStream = null; 
    this.startTime = System.currentTimeMillis();
    this.endTime = startTime + timeout * 1000;
  }

  public GetFileTasks() {
  }

}

