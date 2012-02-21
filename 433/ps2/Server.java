import java.io.*;
import java.net.*;
import java.util.*;

public abstract class Server implements Runnable {

  public int serverPort = 6789;
  public String serverName = "GenericServer";
  ServerSocket listenSocket;
  public boolean alive = true;
  public String documentRoot = "./";

  public void run()
  {
    try {
      this.handleRequests();
    } catch (Exception e) {e.printStackTrace(); }
  }

  public abstract void handleRequests() throws IOException;


  public void setDocumentRoot(String dirname) throws IOException {
    if ((new File(dirname)).exists())
      this.documentRoot = dirname;
    else
      throw new FileNotFoundException("Couldn't open document root: " + dirname);
  }

  public Server (ServerSocket sock, String serverName, String documentRoot) throws IOException {
    this.listenSocket = sock;
    this.serverName = serverName;
    this.setDocumentRoot(documentRoot);

    System.out.println("server listening at: " + listenSocket);
    System.out.println("server www root: " + documentRoot);
  }

  public Server () {
  }


}
