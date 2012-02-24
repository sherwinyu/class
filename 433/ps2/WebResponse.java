// package com.sherwinyu.cs433.ps2;
package syu;

import java.util.*;

public class WebResponse {

  /*
     HTTP/1.0 <status code> <message>
Date: <date>
Server: <your server name>
Content-Type: text/html
Content-Length: <length of file>
CRLF
<file content>
*/

  int statusCode;
  String message;
  String server;
  String contentType;
  long contentLength;
  byte[] content;

  public WebResponse(String status)
  {

  }
  public WebResponse() {
  }
  public boolean equals(WebResponse other)
  {
    if (this.statusCode != other.statusCode) return false;
    if (!this.message.equals(other.message)) return false;
    if (!this.server.equals(other.server)) return false;
    if (!this.contentType.equals(other.contentType)) return false;
    if (this.contentLength != other.contentLength) return false;
    if (!Arrays.equals(this.content, other.content)) return false;
    return true;
  }

  public static WebResponse okResponse(String server) {
    WebResponse resp = new WebResponse();
    resp.statusCode = 200;
    resp.message = "OK";
    resp.server = server;
    return resp;
  }

  public static WebResponse okResponse(String server, String contentType, int length, byte[] content) {
    WebResponse resp = new WebResponse();
    resp.statusCode = 200;
    resp.message = "OK";
    resp.server = server;
    resp.contentType = contentType;
    resp.contentLength = length;
    resp.content = Arrays.copyOf(content, content.length);
    return resp;
  }

  public static WebResponse serverOverloadedResponse(String server) {
    WebResponse resp = new WebResponse();
    resp.statusCode = 502;
    resp.message = "Service temporarily overloaded";
    resp.server = server;
    return resp;
  }


  public static WebResponse notModifiedResponse(String server) {
    WebResponse resp = new WebResponse();
    resp.statusCode = 304;
    resp.message = "Not Modified";
    resp.server = server;
    return resp;
  }
  public static WebResponse fileNotFoundResponse(String server) {
    WebResponse resp = new WebResponse();
    resp.statusCode = 404;
    resp.message = "File Not Found";
    resp.server = server;
    return resp;
  }

  public static WebResponse internalServerErrorResponse(String server) {
    WebResponse resp = new WebResponse();
    resp.statusCode = 500;
    resp.message = "Internal Server Error";
    resp.server = server;
    return resp;
  }

  public static WebResponse badRequestResponse(String server) {
    WebResponse resp = new WebResponse();
    resp.statusCode = 400;
    resp.message = "Bad Request";
    resp.server = server;
    return resp;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("HTTP/1.0 ").append(this.statusCode).append(" ").append(this.message).append("\r\n");
    sb.append("Server: ").append(this.server).append("\r\n");
    if (contentType != null)
    {
      sb.append("Content-Type: ").append(this.contentType).append("\r\n");
      sb.append("Content-Length: ").append(this.contentLength).append("\r\n");
    }
    sb.append("\r\n");
    if (content != null) {
      sb.append(new String(this.content));
    }
    return sb.toString();
  }
}
