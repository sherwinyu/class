import java.io.*;
import java.net.*;
import java.util.*;

public abstract class Server implements Runnable {

  public String serverName;
  public static final String NAME = "AbstractServer";
  ServerSocket listenSocket;
  public boolean alive;
  public String documentRoot;

  public void run()
  {
    try {
      this.handleRequests();
    } catch (Exception e) {e.printStackTrace(); }
  }

  public abstract void handleRequests() throws IOException;

  public Socket acceptIncomingConnection() throws IOException {
    System.out.println("\nAccepting new connection...");
    Socket socket =  listenSocket.accept();
    System.out.println("...Accepted from" +socket.getLocalAddress() + ":" +socket.getPort() );
    return socket;
  }


  public void setDocumentRoot(String dirname) throws IOException {
    if ((new File(dirname)).exists())
      this.documentRoot = dirname;
    else
      throw new FileNotFoundException("Couldn't open document root: " + dirname);
  }

  public Server (ServerSocket sock, String serverName, String documentRoot) throws IOException {
    this.alive = true;
    this.listenSocket = sock;
    this.serverName = serverName;
    this.setDocumentRoot(documentRoot);

    System.out.println("server listening at: " + listenSocket);
    System.out.println("server www root: " + documentRoot);
  }

  public Server () throws IOException {
    this(new ServerSocket(), NAME, ".");
  }


}
