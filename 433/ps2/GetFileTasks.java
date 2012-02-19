package com.sherwinyu.cs433.ps2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

import static com.sherwinyu.cs433.ps2.Utils.*;

public class GetFileTasks implements Runnable {
  protected String server;
  protected int port;
  protected String[] filenames;
  protected int fileInd = 0;

  long startTime;
  long endTime;
  DataOutputStream dataOutputStream;
  Socket socket;
  InetSocketAddress addr;

  public void run () {
    boolean timeup = true;

    while(timeup)
    {
      try {
        Thread.sleep(100);
        setUpConnection();
        nextRequest();
      }
      catch (InterruptedException e) {timeup = false; }
      catch (IOException e) {e.printStackTrace(); }
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
  }

  void writeMessage(String s) throws IOException {
    dataOutputStream.writeBytes(s);
  }

  String receiveResponse() throws IOException {
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

