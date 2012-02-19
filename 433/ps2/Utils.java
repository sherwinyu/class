package com.sherwinyu.cs433.ps2;

public class Utils
{
  public static String inspect(String in)
  {
    String s = "";
    if (in == null) return "NULL";

    for (int i = 0; i < in.length(); i++)
    {

      if (in.charAt(i) == '\n')
        s += "NEWLINE";
      else if (in.charAt(i) == '\r')
        s += "CARRIAGE";
      else
        s += ("" + in.charAt(i));

    }
    return s;
  }
}
