
import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;

public class RequestHandler implements Runnable {

  protected Socket connectionSocket;
  public String WWW_ROOT = "./";
  public String serverName = "someservername";

  public RequestHandler (Socket connectionSocket, String documentRoot, String serverName) {
    this.connectionSocket = connectionSocket;
    this.WWW_ROOT = documentRoot;
    this.serverName = serverName;
  }

  @Override
  public void run() {
    try {
      this.handleRequest();
    } catch (Exception e) { e.printStackTrace(); }
  }

  public void handleRequest() throws IOException {

      String requestString = readRequest( connectionSocket.getInputStream() );
      WebResponse resp;

      WebRequest req = new WebRequest();
      if (!req.fromString(requestString)) { // check if there are parse errors
        resp = WebResponse.badRequestResponse(serverName);
      }
      else {
        resp = generateResponse(req);
      }
      String respString = resp.toString();
      try {
        writeResponse(respString, new DataOutputStream(connectionSocket.getOutputStream()));
        connectionSocket.close();
      }
      catch (SocketException e) {System.out.println("...Client hung up"); }
  }

  protected String readRequest(InputStream in) throws IOException {

    BufferedReader br = new BufferedReader( new InputStreamReader(in));
    StringBuffer sb = new StringBuffer();

    String line = br.readLine();
    System.out.println(line);

    while(line != null && !line.equals(""))
    {
      System.out.println(line);
      sb.append(line + "\r\n");
      line = br.readLine();
    }
    System.out.println("...request= " + sb.toString());
    return sb.toString();
  }

  protected WebResponse generateResponse(WebRequest req) {
    WebResponse resp = new WebResponse();
    if (req.urlName.equals("load"))
      return WebResponse.serverOverloadedResponse(serverName);

    File f = new File(WWW_ROOT, req.urlName);
    if (!f.exists()) // 2. If doesn't exist -> return 404
      return WebResponse.fileNotFoundResponse(serverName);
    if (req.ifModifiedSince != null) // 3. If Modified Since -> return 304
      if( f.lastModified() < req.ifModifiedSince.getTime() ) // if server's file is older
        return WebResponse.notModifiedResponse(serverName);
    try {
      return respondWithFile(f); // 4. Return file (checks cache automatically)
    } catch (Exception e) {
      return WebResponse.internalServerErrorResponse(serverName); // Otherwise, return internal server error
    }
    // return WebResponse.internalServerErrorResponse(serverName); // Otherwise, return internal server error
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

    return WebResponse.okResponse(serverName, contentType, length, content);
  }

  protected void writeResponse(String responseString, DataOutputStream out) throws IOException {
    // System.out.println("...writing response: " + WebRequest.inspect(responseString));
    out.writeBytes(responseString);
  }
}

