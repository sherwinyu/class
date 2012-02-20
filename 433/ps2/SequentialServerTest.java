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
      cal = Calendar.getInstance();
    }

  @Test
    public void createFromArgs() {
      // manually tested
    }

  @Test
    public void testHandleRequests() throws IOException, InterruptedException {

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


      RequestHandler rh = new RequestHandler(mockSocket, ssSpy.WWW_ROOT, ssSpy.serverName);
      RequestHandler rhSpy = spy(rh);

      doReturn(mockSocket).when(ssSpy).acceptIncomingConnection();
      doReturn(rhSpy).when(ssSpy).getRequestHandler(any(Socket.class), anyString(), anyString());
      doReturn(req1.toString()).doReturn(req2.toString()).doReturn(req3.toString()).when(rhSpy).readRequest(any(InputStream.class));
      doNothing().when(rhSpy).writeResponse(anyString(), any(DataOutputStream.class));

      (new Thread(ssSpy)).start();
      Thread.sleep(50);
      ssSpy.alive = false;

      ArgumentCaptor<WebRequest> reqCaptor = ArgumentCaptor.forClass(WebRequest.class);
      ArgumentCaptor<String> respCaptor = ArgumentCaptor.forClass(String.class);

      verify(rhSpy, atLeast(3)).generateResponse(reqCaptor.capture());
      List<WebRequest> reqs = reqCaptor.getAllValues();
      // System.out.println("request 1: " + req1.describe());
      // System.out.println("actual 1: " + reqs.get(0).describe());
      // System.out.println("cmp = " + req1.equals(reqs.get(0)));

      assertEquals(req1.toString(), reqs.get(0).toString());
      assertEquals(req2.toString(), reqs.get(1).toString());
      assertEquals(req3.toString(), reqs.get(2).toString());

      verify(rhSpy, atLeast(3)).writeResponse(respCaptor.capture(), any(DataOutputStream.class));
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

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(SequentialServerTest.class);
  }

}
