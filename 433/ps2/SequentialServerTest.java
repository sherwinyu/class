// package com.sherwinyu.cs433.ps2;

import org.junit.Test;
import org.junit.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.Arrays;

import org.mockito.stubbing.*;
import org.mockito.invocation.*;
import org.mockito.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.verification.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class SequentialServerTest {

  private ServerSocket mockServerSocket;
  private Socket mockSocket;
  private SequentialServer ss;
  private SequentialServer ssSpy;
  private Calendar cal;
  private WebResponse resp;
  private WebRequest req;

  @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

  @Before
    public void setUp() throws IOException {

      mockServerSocket = mock(ServerSocket.class);
      mockSocket = mock(Socket.class);

      ss = new SequentialServer();
      ss.serverPort = 333;
      ss.setDocumentRoot(tmp.getRoot().getPath());
      ssSpy = spy(ss);
      cal=  Calendar.getInstance();
    }

  @Test
    public void createFromArgs() {
      // manually tested
    }

  @Test
    public void handleRequests() throws IOException, InterruptedException {

      WebRequest req1 = new WebRequest("testfile");
      WebRequest req2 = new WebRequest("testfile.jpg");
      WebRequest req3 = new WebRequest("testfile.doesntexist");

      File f = tmp.newFile("testfile");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("herpderp\n");
      bw.close();

      f = tmp.newFile("testfile.jpg");
      bw = new BufferedWriter(new FileWriter(f));
      bw.write("herpderp\n");
      bw.close();

      doReturn(mockSocket).when(ssSpy).acceptIncomingConnection();
      doReturn(req1.toString()).doReturn(req2.toString()).doReturn(req3.toString()).when(ssSpy).readRequest(any(InputStream.class));
      doNothing().when(ssSpy).writeResponse(anyString(), any(DataOutputStream.class));

      (new Thread(ssSpy)).start();
      Thread.sleep(50);
      ssSpy.alive = false;

      ArgumentCaptor<WebRequest> reqCaptor = ArgumentCaptor.forClass(WebRequest.class);
      ArgumentCaptor<String> respCaptor = ArgumentCaptor.forClass(String.class);

      verify(ssSpy, atLeast(3)).generateResponse(reqCaptor.capture());
      List<WebRequest> reqs = reqCaptor.getAllValues();
      System.out.println("request 1: " + req1.describe());
      System.out.println("actual 1: " + reqs.get(0).describe());
      System.out.println("cmp = " + req1.equals(reqs.get(0)));

      assertEquals(req1.toString(), reqs.get(0).toString());
      assertEquals(req2.toString(), reqs.get(1).toString());
      assertEquals(req3.toString(), reqs.get(2).toString());

      verify(ssSpy, atLeast(3)).writeResponse(respCaptor.capture(), any(DataOutputStream.class));
      List<String> resps = respCaptor.getAllValues();
      // System.out.println("request 1: " + req1.inspect());
      // System.out.println("actual 1: " + reqs.get(0).inspect());
      // System.out.println("cmp = " + req1.equals(reqs.get(0)));

      WebResponse resp1 = WebResponse.okResponse(ssSpy.serverName, "text/plain", 9, "herpderp\n".getBytes()) ;
      WebResponse resp2 = WebResponse.okResponse(ssSpy.serverName, "image/jpeg", 9, "herpderp\n".getBytes()) ;
      WebResponse resp3 = WebResponse.fileNotFoundResponse(ssSpy.serverName);

      assertEquals(resp1.toString(), resps.get(0).toString());
      assertEquals(resp2.toString(), resps.get(1).toString());
      assertEquals(resp3.toString(), resps.get(2).toString());
    }

  @Test
    public void testGenerateResponseLoad() {
      WebResponse resp = ss.generateResponse(new WebRequest("load"));
      assertEquals("Service temporarily overloaded", resp.message);
      assertEquals(502, resp.statusCode);
      assertEquals(null, resp.contentType);
      assertArrayEquals(null, resp.content);
    }

  @Test
    public void testGenerateResponseIfModifiedHeader() throws IOException {
      WebRequest req = new WebRequest("testfile");
      cal.set(2012, 01, 01);
      req.ifModifiedSince = cal.getTime();
      ss.setDocumentRoot(tmp.getRoot().getPath());

      File f = tmp.newFile("testfile");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("herpderp\n");
      bw.close();

      // Server has newer file
      cal.set(2012, 02, 01);
      f.setLastModified(cal.getTimeInMillis());
      WebResponse resp = ss.generateResponse(req);
      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals("text/plain", resp.contentType);
      assertArrayEquals("herpderp\n".getBytes(), resp.content);

      // Server has older file
      cal.set(2000, 01, 01);
      f.setLastModified(cal.getTimeInMillis());
      resp = ss.generateResponse(req);
      assertEquals("Not Modified", resp.message);
      assertEquals(304, resp.statusCode);
      assertEquals(null, resp.contentType);
      assertArrayEquals(null, resp.content);
    }

  @Test
    public void testGenerateResponseFileNotFound() throws IOException {
      req = new WebRequest("nonexistentfile");
      resp = ss.generateResponse(req);
      assertEquals("File Not Found", resp.message);
      assertEquals(404, resp.statusCode);
      assertEquals(null, resp.contentType);
      assertArrayEquals(null, resp.content);
    }

  @Test
    public void respondWithFileJpg() throws IOException {
      ss.setDocumentRoot(tmp.getRoot().getPath());

      File f = tmp.newFile("testfile.jpg");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("jpegfile\n");
      bw.close();

      req = new WebRequest("testfile.jpg");
      resp = ss.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals("image/jpeg", resp.contentType);
      assertArrayEquals("jpegfile\n".getBytes(), resp.content);
      System.out.println("actual=" + Arrays.toString(resp.content));
      System.out.println("expected=" + Arrays.toString("jpegfile\n".getBytes()));
      assertEquals(WebResponse.okResponse(ss.serverName, "image/jpeg", 9, "jpegfile\n".getBytes()).toString(), resp.toString());
    }

  @Test
    public void respondWithFileGif() throws IOException {
      ss.setDocumentRoot(tmp.getRoot().getPath());

      File f = tmp.newFile("testfile.gif");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("gifdata\n");
      bw.close();

      req = new WebRequest("testfile.gif");
      resp = ss.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals("image/gif", resp.contentType);
      assertArrayEquals("gifdata\n".getBytes(), resp.content);
    }

  @Test
    public void respondWithFileHtml() throws IOException {
      ss.setDocumentRoot(tmp.getRoot().getPath());

      File f = tmp.newFile("testfile.html");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("<h1>hello!</h1>\n");
      bw.close();

      req = new WebRequest("testfile.html");
      resp = ss.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals("text/html", resp.contentType);
      assertArrayEquals("<h1>hello!</h1>\n".getBytes(), resp.content);
    }

  @Test
    public void respondWithFilePlain() throws IOException {
      ss.setDocumentRoot(tmp.getRoot().getPath());

      File f = tmp.newFile("testfile");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("<h1>hello! this file is plaintext</h1>\n");
      bw.close();

      req = new WebRequest("testfile");
      resp = ss.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals("text/plain", resp.contentType);
      assertArrayEquals("<h1>hello! this file is plaintext</h1>\n".getBytes(), resp.content);
    }
  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(SequentialServerTest.class);
  }

}
