import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class WebRequest {
  static SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
  String method;
  String urlName;
  Date ifModifiedSince;
  String userAgent;

  /*
   * Fills the web request with contents from the input string
   */
  public boolean fromString(String in) {
    String[] lines = in.split("\r\n"); // HTTP standard requires \r\n, but most servers accept just \n as line ender;
    try {
      if (lines.length < 1)
        return false;

      String[] requestLineTokens = lines[0].split("\\s+");

      if (requestLineTokens.length < 3 || !requestLineTokens[0].equals("GET") || !requestLineTokens[2].equals("HTTP/1.0")) {
        return false;
      }
      this.method = requestLineTokens[0];
      this.urlName = requestLineTokens[1];

      for(int i = 1; i < lines.length; i++) {
        String[] tokens = lines[i].split("\\s+", 2);
        // System.out.println(lines[i]);
        // System.out.println(Arrays.toString(tokens));
        if (tokens[0].startsWith("If-Modified-Since") ) {
          this.ifModifiedSince = format.parse(tokens[1]);
        }
        if (tokens[0].startsWith("User-Agent")) {
          this.userAgent = tokens[1];
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("Bad request!");
      return false;
    } catch (ParseException e) {
      System.out.println("Bad request!");
      return false;
    }

    return true;

  }

  public WebRequest() {
  }

  /* String dateString = "Wed, 09 Apr 2008 23:55:38 GMT";
   * SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
   * Date d = format.parse(dateString);
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(this.method).append(" ").append(this.urlName).append(" HTTP/1.0\r\n");
    if (ifModifiedSince != null)
      sb.append("If-Modified-Since: ").append(format.format(ifModifiedSince)).append("\r\n");
    if (userAgent != null)
      sb.append("User-Agent: ").append(userAgent).append("\r\n");
    sb.append("\r\n");
    return sb.toString();
  }

  public WebRequest(String urlName) {
    this.method = "GET";
    this.urlName = urlName;
  }

}
