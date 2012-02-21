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
import static syu.Utils.*;


public class ThreadPerRequestServerTest {

  private ServerSocket mockServerSocket;
  private Socket mockSocket;
  private ThreadPerRequestServer server;
  private ThreadPerRequestServer serverSpy;
  private Calendar cal;
  private WebResponse resp;
  private WebRequest req;

  @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

  @Before
    public void setUp() throws IOException {

      mockServerSocket = mock(ServerSocket.class);
      mockSocket = mock(Socket.class);

      server = new ThreadPerRequestServer();
      server.setDocumentRoot(tmp.getRoot().getPath());
      serverSpy = spy(server);
      cal = Calendar.getInstance();
    }

  @Test
    public void createFromArgs() {
      // manually tested
    }

  @Test
    public void testStartNewThreadCalls() throws IOException, InterruptedException {

      RequestHandler rh = new RequestHandler(serverSpy, mockSocket);
      RequestHandler rhSpy = spy(rh);

      doReturn(mockSocket).when(serverSpy).acceptIncomingConnection();
      doReturn("someString").when(rhSpy).readRequest(any(InputStream.class));
      doNothing().when(rhSpy).writeResponse(anyString(), any(DataOutputStream.class));
      doReturn(rhSpy).when(serverSpy).getRequestHandler(any(Socket.class));

      (new Thread(serverSpy)).start();
      Thread.sleep(200);
      serverSpy.alive = false;
      verify(serverSpy, atLeast(25)).getRequestHandler(any(Socket.class));
      verify(serverSpy, atLeast(25)).startNewThread(rhSpy);
    }

  @Test
    public void integrationTestHandlRequests() throws IOException, InterruptedException {

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

      RequestHandler rh1 = new RequestHandler(serverSpy, mockSocket);
      RequestHandler rh1Spy = spy(rh1);

      RequestHandler rh2 = new RequestHandler(serverSpy, mockSocket);
      RequestHandler rh2Spy = spy(rh2);

      RequestHandler rh3 = new RequestHandler(serverSpy, mockSocket);
      RequestHandler rh3Spy = spy(rh3);

      doReturn(mockSocket).doReturn(mockSocket).doReturn(mockSocket).doAnswer(
        new Answer() {
          public Socket answer(InvocationOnMock inv) {
            try {
            Thread.sleep(1000); // simulate blocking after three requests
            } catch (InterruptedException e) {
              System.out.println("interrupted." + e.getMessage());
            }
            return null;
          }
        }
      ).when(serverSpy).acceptIncomingConnection();

      doReturn(rh1Spy).doReturn(rh2Spy).doReturn(rh3Spy).when(serverSpy).getRequestHandler(any(Socket.class));
      doReturn(req1.toString()).when(rh1Spy).readRequest(any(InputStream.class));
      doReturn(req2.toString()).when(rh2Spy).readRequest(any(InputStream.class));
      doReturn(req3.toString()).when(rh3Spy).readRequest(any(InputStream.class));

      doNothing().when(rh1Spy).writeResponse(anyString(), any(DataOutputStream.class));
      doNothing().when(rh2Spy).writeResponse(anyString(), any(DataOutputStream.class));
      doNothing().when(rh3Spy).writeResponse(anyString(), any(DataOutputStream.class));

      (new Thread(serverSpy)).start();
      Thread.sleep(100);
      serverSpy.alive = false;

      ArgumentCaptor<WebRequest> req1Captor = ArgumentCaptor.forClass(WebRequest.class);
      ArgumentCaptor<WebRequest> req2Captor = ArgumentCaptor.forClass(WebRequest.class);
      ArgumentCaptor<WebRequest> req3Captor = ArgumentCaptor.forClass(WebRequest.class);

      ArgumentCaptor<String> resp1Captor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> resp2Captor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> resp3Captor = ArgumentCaptor.forClass(String.class);

      verify(rh1Spy, times(1)).generateResponse(req1Captor.capture());
      verify(rh2Spy, times(1)).generateResponse(req2Captor.capture());
      verify(rh3Spy, times(1)).generateResponse(req3Captor.capture());

      assertEquals(req1.toString(), req1Captor.getValue().toString());
      assertEquals(req2.toString(), req2Captor.getValue().toString());
      assertEquals(req3.toString(), req3Captor.getValue().toString());

      verify(rh1Spy, times(1)).writeResponse(resp1Captor.capture(), any(DataOutputStream.class));
      verify(rh2Spy, times(1)).writeResponse(resp2Captor.capture(), any(DataOutputStream.class));
      verify(rh3Spy, times(1)).writeResponse(resp3Captor.capture(), any(DataOutputStream.class));

      WebResponse resp1 = WebResponse.okResponse(serverSpy.serverName, "text/plain", 9, "herpderp\n".getBytes()) ;
      WebResponse resp2 = WebResponse.okResponse(serverSpy.serverName, "image/jpeg", 9, "herpderp\n".getBytes()) ;
      WebResponse resp3 = WebResponse.fileNotFoundResponse(serverSpy.serverName);

      assertEquals(resp1.toString(), resp1Captor.getValue());
      assertEquals(resp2.toString(), resp2Captor.getValue());
      assertEquals(resp3.toString(), resp3Captor.getValue());
    }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(ThreadPerRequestServerTest.class);
  }

}
