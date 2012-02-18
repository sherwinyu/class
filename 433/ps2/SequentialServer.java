import java.io.*;
import java.net.*;
import java.util.*;

public class SequentialServer implements Runnable{

  public int serverPort = 6789;
  public String serverName = "SequentialServer";
  ServerSocket listenSocket;
  public boolean alive = true;

  //public static String WWW_ROOT = "/home/httpd/html/zoo/classes/cs433/";
  public String WWW_ROOT = "./";


  // %java <servername> -config <config_file_name>
  public static SequentialServer createFromArgs(String[] args) throws Exception {

    SequentialServer ss = new SequentialServer();
    if (args.length !=2 || !args[0].equals("-config")) {
      throw new NumberFormatException();
    }
    BufferedReader br = new BufferedReader(new FileReader(args[1]));
    while(br.ready()) {
      String[] toks = br.readLine().split("\\s+");
      if (toks[0].equals("Listen"))
        ss.serverPort = Integer.parseInt(toks[1]);
      if (toks[0].equals("DocumentRoot"))
        ss.setDocumentRoot(toks[1]);
    }

    ss.listenSocket = new ServerSocket(ss.serverPort);
    System.out.println("server listening at: " + ss.serverPort);
    System.out.println("server www root: " + ss.WWW_ROOT);

    return ss;
  }

  public void setDocumentRoot(String dirname) throws IOException {
    if ((new File(dirname)).exists())
      this.WWW_ROOT = dirname;
    else
      throw new FileNotFoundException("Couldn't open document root: " + dirname);
  }

  public void run()
  {
    try {
    this.handleRequests();
    } catch (Exception e) {e.printStackTrace(); }
  }


  public void handleRequests() throws IOException {

    Socket connectionSocket;
    while (alive) {
      connectionSocket = acceptIncomingConnection(); // blocking

      String requestString = readRequest( connectionSocket.getInputStream() ); //new BufferedReader( new InputStreamReader(connectionSocket.getInputStream())));

      WebResponse resp;
      WebRequest req = new WebRequest();
      if (!req.fromString(requestString)) { // check if there are parse errors
        resp = WebResponse.badRequestResponse(serverName);
      }
      else // no parse errors
      {
        resp = generateResponse(req);
      }
      String respString = resp.toString();
      writeResponse(respString, new DataOutputStream(connectionSocket.getOutputStream()));
    }

  }

  public Socket acceptIncomingConnection() throws IOException {
    return listenSocket.accept();
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
    //System.out.println("request= " + sb.toString());
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
    // System.out.println("content from file: " + f.getPath() + "\t" + new String(content));

    return WebResponse.okResponse(serverName, contentType, length, content);
  }

  protected void writeResponse(String responseString, DataOutputStream out) throws IOException {
    out.writeBytes(responseString);
  }



  public static void main(String args[]) throws Exception  {

    try {
      SequentialServer ss = createFromArgs(args);
      (new Thread(ss)).start();
    }
    catch (NumberFormatException e)
    {
      System.out.println("Usage: java SequentialServer -config <config_file>");
    }
    catch (FileNotFoundException e)
    {
      System.out.println(e.getMessage());
    }
    catch (BindException e)
    {
      System.out.println("Port unavailable");
    }


    // see if we do not use default server port


  } // end of main

} // end of class SimpleWebServer
