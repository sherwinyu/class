package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;

public class SyncRequestHandler extends RequestHandler implements Runnable {

  protected Socket connectionSocket;

  public SyncRequestHandler (Server server, Socket connectionSocket) {
    this.parentServer = server;
    this.connectionSocket = connectionSocket;
  }

  public SyncRequestHandler() {
    parentServer = null;
    connectionSocket = null;
  }

  /*
     public RequestHandler (Socket connectionSocket, String documentRoot, String serverName) {
     this.connectionSocket = connectionSocket;
     this.serverName = serverName;
     }
     */

  @Override
    public void run() {
      try {
        this.handleRequest();
      } catch (Exception e) { e.printStackTrace(); }
    }


  public void handleRequest() throws IOException {
    String requestString = readRequest( connectionSocket.getInputStream() );
    p(this,"Read FRM: " + connectionSocket.toString() + " \t request: " + preview(requestString));
    String respString = getResponseString(requestString);

    // String respString = resp.toString();
    try {
      p(this,"Write TO: " + this.connectionSocket.toString() + "\t response:" +  preview(respString));
      writeResponse(respString, new DataOutputStream(connectionSocket.getOutputStream()));
      connectionSocket.close();
      p(this, "connection closed");

    }
    catch (SocketException e) {p(this, "client (" + connectionSocket + ") hung up"); }
  }

  protected String readRequest(InputStream in) throws IOException {
    BufferedReader br = new BufferedReader( new InputStreamReader(in));
    StringBuffer sb = new StringBuffer();

    String line = br.readLine();

    while(line != null && !line.equals(""))
    {
      sb.append(line + "\r\n");
      line = br.readLine();
    }
    return sb.toString();
  }

  protected void writeResponse(String responseString, DataOutputStream out) throws IOException {
    out.writeBytes(responseString);
  }

}

