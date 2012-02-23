package syu;

import java.io.*;
import java.util.*;
import java.nio.channels.*;
// import static Server.*;
// import static RequestHandler.*;

public class Utils
{
  public static String inspect(String in)
  {
    String s = "";
    if (in == null) return "NULL";

    for (int i = 0; i < in.length(); i++) {
      if (in.charAt(i) == '\n')
        s += "\\n";
      else if (in.charAt(i) == '\r')
        s += "\\r";
      else
        s += ("" + in.charAt(i));
    }
    return s;
  }

  public static void p(String in) {
    System.out.println(in);
  }

  public static void pp(String in) {
    System.out.println(in);
  }

  public static void ppp(String in) {
    System.out.println(in);
  }


  public static HashMap<String, String> parseArgs(String[] args) throws FileNotFoundException, IOException {

    HashMap<String, String> h = new HashMap<String, String>();

    if (args.length !=2 || !args[0].equals("-config")) {
      throw new NumberFormatException();
    }

    String[] attributes = new String[] { "Listen", "DocumentRoot", "ThreadPoolSize", "CacheSize" };

    BufferedReader br = new BufferedReader(new FileReader(args[1]));
    while(br.ready()) {
      String[] toks = br.readLine().split("\\s+");
      for (String atr : attributes) {
        if (toks[0].equals(atr))
          h.put(atr,  toks[1]);
      }
    }
    return h;
  }

  public static void p(Server server, String s) {
    p(server, 2, s);
  }

  public static void p(Server server, int depth, String s) {
    System.out.println(indent(depth) + server.serverName + ":\t" +inspect(s));
  }

  public static void p(GetFileTasks o, String s) {
    p(o, 2, s);
  }

  public static void p(GetFileTasks o, int depth, String s) {
    System.out.println(indent(depth) + o.id + ":\t" +inspect(s));
  }

  public static void p(Debuggable o, String s) {
    p(o, 2, s);
  }

  public static void p(Debuggable o, int depth, String s) {
    System.out.println(indent(depth) + o.id() + ":\t" +inspect(s));
  }

  public static String port(SocketChannel sc) {
    return sc.socket().getPort() + "";
  }

/*
  public static void p(RequestHandler rh, String s) {
    p(rh, 2, s);
  }

  public static void p(RequestHandler rh, int depth, String s) {
    p(indent(depth) + rh.id + ":\t" +inspect(s));
  }


  public static void p(Dispatcher o, String s) {
    p(o, 2, s);
  }

  public static void p(Dispatcher o, int depth, String s) {
    System.out.println(indent(depth) + o.id + ":\t" +inspect(s));
  }



  */
  private static  String indent(int n) {
    String s = "";
    for(int i = 0; i < n; i++)
      s += "..";
    return s;
  }
}

interface Debuggable {
  public String id();
}


