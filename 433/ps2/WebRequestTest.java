// package com.sherwinyu.cs433.ps2;

import org.junit.Test;
import org.junit.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.text.*;

import org.mockito.stubbing.*;
import org.mockito.invocation.*;
import org.mockito.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.verification.*;

public class WebRequestTest {

  private ServerSocket mockServerSocket;
  private Socket mockSocket;
  private SequentialServer ss;
  private SequentialServer ssSpy;

  private WebRequest req;

  @Before
    public void setUp() throws UnknownHostException {
      req = new WebRequest();

      mockServerSocket = mock(ServerSocket.class);
      mockSocket = mock(Socket.class);

      ss = new SequentialServer();
      ss.serverPort = 333;
      ssSpy = spy(ss);

    }

  @Test
    public void testFromString() {
      String str;
      boolean status;

      str = "GET asdfasdf HTTP/1.0\r\nHost: yoursever.com\r\nAccept:asdf\r\nUser-Agent: Mozilla\r\n";
      status = req.fromString(str);
      assertTrue("Valid request", status);
      assertEquals("GET", req.method);
      assertEquals("asdfasdf", req.urlName);
      assertEquals("Mozilla", req.userAgent);
      assertEquals(null, req.ifModifiedSince);

      str = "GETZ asdfasdf HTTP/1.0\r\nHost: yoursever.com\r\nAccept:asdf\r\nUser-Agent: Mozilla\r\n";
      req = new WebRequest();
      status = req.fromString(str);
      assertFalse("Valid request", status);
      assertEquals(null, req.method);
      assertEquals(null, req.urlName);
      assertEquals(null, req.userAgent);
      assertEquals(null, req.ifModifiedSince);

      str = "GET file/dir HTTP/1.0\r\nHost: yoursever.com\r\nIf-Modified-Since: Wed, 09 Apr 2008 23:55:38 GMT\r\nUser-Agent: Mozilla\r\n";
      req = new WebRequest();
      status = req.fromString(str);
      assertTrue("Valid request", status);
      assertEquals("GET", req.method);
      assertEquals("file/dir", req.urlName);
      assertEquals("Mozilla", req.userAgent);
      try {
        Date d = WebRequest.format.parse("Wed, 09 Apr 2008 23:55:38 GMT");
        assertEquals(d, req.ifModifiedSince);
      } catch(ParseException e) {e.printStackTrace();}
    }
  @Test
    public void testToStringWithBothUserAgentAndIfModifiedSince() {
      req.method = "GET";
      req.urlName = "some_file#tag/slash.jpg";
      req.userAgent = "testagent";
      Date  d = new Date();
      req.ifModifiedSince = d;
      String expected = "GET some_file#tag/slash.jpg HTTP/1.0\r\nIf-Modified-Since: " + WebRequest.format.format(d) + "\r\nUser-Agent: testagent\r\n\r\n";
      assertEquals("When both userAgent and ifModifiedSince are present", expected, req.toString());

    }

  @Test
    public void testToStringIfModifiedSinceOnly() {
      req.method = "GET";
      req.urlName = "some_file#tag/slash.jpg";
      Date  d = new Date();
      req.ifModifiedSince = d;
      String expected = "GET some_file#tag/slash.jpg HTTP/1.0\r\nIf-Modified-Since: " + WebRequest.format.format(d) + "\r\n\r\n";
      assertEquals("Only ifModifiedSince header", expected, req.toString());
    }

  @Test
    public void testToStringUserAgentOnly() {
      req.method = "GET";
      req.urlName = "some_file#tag/slash.jpg";
      req.userAgent = "testagent";
      String expected = "GET some_file#tag/slash.jpg HTTP/1.0\r\nUser-Agent: testagent\r\n\r\n";
      assertEquals("Only UserAgent header", expected, req.toString());
    }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(WebRequestTest.class);
  }

  public static void main(String[] args) throws Exception {
    WebRequestTest t = new WebRequestTest();
    t.setUp();
    t.testFromString();

  }
}
