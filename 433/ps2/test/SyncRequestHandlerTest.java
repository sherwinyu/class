package syu;

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

public class SyncRequestHandlerTest {

  private Calendar cal;
  private WebResponse resp;
  private WebRequest req;
  private Socket mockSocket;
  private ServerSocket mockServerSocket;
  private Server mockServer;

  private SyncRequestHandler rh;

  @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

  @Before
    public void setUp() throws IOException {

      // mockServerSocket = mock(ServerSocket.class);
      mockSocket = mock(Socket.class);

      // ss = new SequentialServer();
      mockServer = mock(Server.class, CALLS_REAL_METHODS);
      mockServer.setDocumentRoot(tmp.getRoot().getPath());
      mockServer.serverName = "GenericServeName";

      rh = new SyncRequestHandler(mockServer, mockSocket);

      cal =  Calendar.getInstance();
    }

  @Test
    public void testGenerateResponseLoad() {
      WebResponse resp = rh.generateResponse(new WebRequest("load"));
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

      File f = tmp.newFile("testfile");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("herpderp\n");
      bw.close();

      // Server has newer file
      cal.set(2012, 02, 01);
      f.setLastModified(cal.getTimeInMillis());
      WebResponse resp = rh.generateResponse(req);
      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals("text/plain", resp.contentType);
      assertArrayEquals("herpderp\n".getBytes(), resp.content);

      // Server has older file
      cal.set(2000, 01, 01);
      f.setLastModified(cal.getTimeInMillis());
      resp = rh.generateResponse(req);
      assertEquals("Not Modified", resp.message);
      assertEquals(304, resp.statusCode);
      assertEquals(null, resp.contentType);
      assertArrayEquals(null, resp.content);
    }

  @Test
    public void testGenerateResponseFileNotFound() throws IOException {
      req = new WebRequest("nonexistentfile");
      resp = rh.generateResponse(req);
      assertEquals("File Not Found", resp.message);
      assertEquals(404, resp.statusCode);
      assertEquals(null, resp.contentType);
      assertArrayEquals(null, resp.content);
    }

  @Test
    public void testRespondWithFileJpg() throws IOException {

      File f = tmp.newFile("testfile.jpg");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("jpegfile\n");
      bw.close();

      req = new WebRequest("testfile.jpg");
      resp = rh.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals(mockServer.serverName, resp.server);
      assertEquals("image/jpeg", resp.contentType);
      assertArrayEquals("jpegfile\n".getBytes(), resp.content);
      System.out.println("actual=" + Arrays.toString(resp.content));
      System.out.println("expected=" + Arrays.toString("jpegfile\n".getBytes()));
      assertEquals(WebResponse.okResponse(rh.parentServer.serverName, "image/jpeg", 9, "jpegfile\n".getBytes()).toString(), resp.toString());
    }

  @Test
    public void testRespondWithFileGif() throws IOException {

      File f = tmp.newFile("testfile.gif");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("gifdata\n");
      bw.close();

      req = new WebRequest("testfile.gif");
      resp = rh.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals(mockServer.serverName, resp.server);
      assertEquals("image/gif", resp.contentType);
      assertArrayEquals("gifdata\n".getBytes(), resp.content);
    }

  @Test
    public void testRespondWithFileHtml() throws IOException {

      File f = tmp.newFile("testfile.html");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("<h1>hello!</h1>\n");
      bw.close();

      req = new WebRequest("testfile.html");
      resp = rh.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals(mockServer.serverName, resp.server);
      assertEquals("text/html", resp.contentType);
      assertArrayEquals("<h1>hello!</h1>\n".getBytes(), resp.content);
    }

  @Test
    public void testRespondWithFilePlain() throws IOException {

      File f = tmp.newFile("testfile");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("<h1>hello! this file is plaintext</h1>\n");
      bw.close();

      req = new WebRequest("testfile");
      resp = rh.generateResponse(req);

      assertEquals("OK", resp.message);
      assertEquals(200, resp.statusCode);
      assertEquals(mockServer.serverName, resp.server);
      assertEquals("text/plain", resp.contentType);
      assertArrayEquals("<h1>hello! this file is plaintext</h1>\n".getBytes(), resp.content);
    }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(SyncRequestHandlerTest.class);
  }

  }
