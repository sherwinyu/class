package syu;

import java.io.*;
import java.net.*;
import java.util.*;
import static syu.Utils.*;

public abstract class Server implements Runnable {

  public String serverName;
  public static final String NAME = "AbstractServer";
  public boolean alive;
  public String documentRoot;

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

  public Server(String serverName, String documentRoot) throws IOException {
    this.alive = true;
    this.serverName = serverName;
    this.setDocumentRoot(documentRoot);
  }




}
