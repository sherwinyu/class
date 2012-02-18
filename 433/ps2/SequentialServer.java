import java.io.*;
import java.net.*;
import java.util.*;

public class SequentialServer{

  public int serverPort = 6789;
  public String serverName = "SequentialServer";
  ServerSocket listenSocket;

  //public static String WWW_ROOT = "/home/httpd/html/zoo/classes/cs433/";
  public String WWW_ROOT = "./";

  public static SequentialServer createFromArgs(String[] args) throws Exception {

    SequentialServer ss = new SequentialServer();

    if (args.length !=2)
    {
      throw new NumberFormatException();
    }
    if (args.length >= 1)
      ss.serverPort = Integer.parseInt(args[0]);

    // see if we want a different root
    if (args.length >= 2)
      ss.WWW_ROOT = args[1];

    // create server socket
    ss.listenSocket = new ServerSocket(ss.serverPort);
    // System.out.println("server listening at: " + listenSocket);
    // System.out.println("server www root: " + WWW_ROOT);

    return ss;
  }


  public void handleRequests() throws Exception {

    Socket connectionSocket;
    while (true) {
      connectionSocket = acceptIncomingConnection(); // blocking

      String requestString = readRequest( new BufferedReader( new InputStreamReader(connectionSocket.getInputStream())));

      WebResponse resp;
      WebRequest req = new WebRequest();
      if (!req.fromString(requestString)) { // check if there are parse errors
        resp = new WebResponse("ERROR");
      }
      else // no parse errors
      {
        resp = generateResponse(req);
      }
      String respString = resp.toString();
      writeResponse(respString, connectionSocket.getOutputStream());
    }

  }

  public Socket acceptIncomingConnection() throws IOException {
    return listenSocket.accept();
  }

  protected String readRequest(BufferedReader br) throws IOException {
    StringBuffer sb = new StringBuffer();

    String line = br.readLine();
    while(line != null && !line.equals(""))
    {
      sb.append(line + "\r\n");
      line = br.readLine();
    }
    System.out.println("request= " + sb.toString());
    return sb.toString();
  }

  protected WebResponse generateResponse(WebRequest req) {
    WebResponse resp = new WebResponse();


    if (req.urlName.equals("load"))
      return WebResponse.serverOverloadedResponse(serverName);

    // Check cases
    // 2. /Load
    // 1. If Modified Header
    // 3. File doesn't exist -> return error
    // 4. File in cache -> return from cache
    // 5. Return file -> disk look up
    return resp;
  }

  protected void writeResponse(String responseString, OutputStream out) throws IOException {
    DataOutputStream outToClient = new DataOutputStream(out);
    outToClient.writeBytes(responseString);
  }



  public static void main(String args[]) throws Exception  {

    try {
      SequentialServer ss = createFromArgs(args);
      ss.handleRequests();
    }
    catch (NumberFormatException e)
    {
      System.out.println("Usage: java SequentialServer <server address> <port> <server password>");
    }


    // see if we do not use default server port


  } // end of main

} // end of class SimpleWebServer
