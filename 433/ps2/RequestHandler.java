package syu;

import java.io.*;
import java.net.*;
import java.util.*;

import static syu.Utils.*;
public class RequestHandler implements Debuggable {

  protected String id;
  protected Server parentServer;
  protected FileCache sharedCache;

  public String id() {
    return id;
  }

  public String getResponseString(String requestString) {

    WebResponse resp;
    WebRequest req = new WebRequest();
    if (!req.fromString(requestString)) { // check if there are parse errors
      resp = WebResponse.badRequestResponse(parentServer.serverName);
    }
    else {
      resp = generateResponse(req);
    }
    return resp.toString();
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
    byte[] content = null; 

    if( sharedCache != null ) 
      if (sharedCache.contains(f.getPath())) {
        content = sharedCache.get(f.getPath());
      }

    if (content == null) {
      FileInputStream fileStream  = new FileInputStream(f);
      content = new byte[length];
      fileStream.read(content);
      if (sharedCache.roomForFile(content) ) 
        sharedCache.put(f.getPath(), content);
    }



    return WebResponse.okResponse(parentServer.serverName, contentType, length, content);
  }

}
