package syu;
import java.io.*;
import java.util.*;

public class Utils
{
  public static String inspect(String in)
  {
    String s = "";
    if (in == null) return "NULL";

    for (int i = 0; i < in.length(); i++) {
      if (in.charAt(i) == '\n')
        s += "NEWLINE";
      else if (in.charAt(i) == '\r')
        s += "CARRIAGE";
      else
        s += ("" + in.charAt(i));
    }
    return s;
  }

  public static void p(String in) {
    System.out.println(in);
  }

  public static void pr(String in) {
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





}
