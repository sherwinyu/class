package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;

public class RequestHandler implements Runnable {

  protected Socket connectionSocket;
  protected Server parentServer;
  protected String id;

  public RequestHandler (Server server, Socket connectionSocket) {
    this.parentServer = server;
    this.connectionSocket = connectionSocket;
  }

  public RequestHandler() {
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
    p(this,"Read FRM: " + connectionSocket.toString() + " \t request: " + requestString);
    WebResponse resp;

    WebRequest req = new WebRequest();
    if (!req.fromString(requestString)) { // check if there are parse errors
      resp = WebResponse.badRequestResponse(parentServer.serverName);
    }
    else {
      resp = generateResponse(req);
    }
    String respString = resp.toString();
    try {
      writeResponse(respString, new DataOutputStream(connectionSocket.getOutputStream()));
      p(this,"Write TO: " + this.connectionSocket.toString() + "\t response:" +  respString);
      connectionSocket.close();
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

  protected WebResponse generateResponse(WebRequest req) {
    WebResponse resp = new WebResponse();
    if (req.urlName.equals("load"))
      return WebResponse.serverOverloadedResponse(parentServer.serverName);

    File f = new File(parentServer.documentRoot, req.urlName);
    if (!f.exists()) // 2. If doesn't exist -> return 404
      return WebResponse.fileNotFoundResponse(parentServer.serverName);
    if (req.ifModifiedSince != null) // 3. If Modified Since -> return 304
      if( f.lastModified() < req.ifModifiedSince.getTime() ) // if server's file is older
        return WebResponse.notModifiedResponse(parentServer.serverName);
    try {
      return respondWithFile(f); // 4. Return file (checks cache automatically)
    } catch (Exception e) {
      return WebResponse.internalServerErrorResponse(parentServer.serverName); // Otherwise, return internal server error
    }
  }

  /*
   * Precondition: file denoted by fn exists
   * This method provides an abstraction ontop of the cache-disk system
   * Throws IOException if precondition does not hold
   */
  public WebResponse respondWithFile(File f) throws IOException {
    int length = (int) f.length(); //TODO(syu): handle files of size greater than INT_MAX bytes?

    String contentType;
    if (f.getPath().endsWith(".jpg"))
      contentType = "image/jpeg";
    else if (f.getPath().endsWith(".gif"))
      contentType = "image/gif";
    else if (f.getPath().endsWith(".html") || f.getPath().endsWith(".htm"))
      contentType = "text/html";
    else
      contentType = "text/plain";

    FileInputStream fileStream  = new FileInputStream(f);
    byte[] content = new byte[length];
    fileStream.read(content);

    return WebResponse.okResponse(parentServer.serverName, contentType, length, content);
  }

  protected void writeResponse(String responseString, DataOutputStream out) throws IOException {
    out.writeBytes(responseString);
  }
}

