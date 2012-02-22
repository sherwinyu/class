
package syu;

import java.io.*;
import java.net.*;
import java.util.*;
import static syu.Utils.*;

public abstract class SynchronousServer extends Server {

  ServerSocket listenSocket;

  public Socket acceptIncomingConnection() throws IOException {
    p(this, 1 ,"Accepting new connection...");
    Socket socket =  listenSocket.accept();
    p(this, "Accepted from" +socket.getLocalAddress() + ":" +socket.getPort() );
    return socket;
  }

  public SynchronousServer (ServerSocket sock, String serverName, String documentRoot) throws IOException {

    super(serverName, documentRoot);
    this.listenSocket = sock;
    p(this, 1,  "server listening at: " + listenSocket);
    p(this, 1, "server www root: " + documentRoot);
  }

}
