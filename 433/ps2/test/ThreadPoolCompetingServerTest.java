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
import static syu.Utils.*;


public class ThreadPoolCompetingServerTest {

  private ServerSocket mockServerSocket;
  private Socket mockSocket;
  private ThreadPoolCompetingServer server;
  private ThreadPoolCompetingServer serverSpy;
  private WebResponse resp;
  private WebRequest req;

  @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

  @Before
    public void setUp() throws IOException {

      mockServerSocket = mock(ServerSocket.class);
      mockSocket = mock(Socket.class);

      server = new ThreadPoolCompetingServer(10);
      server.setDocumentRoot(tmp.getRoot().getPath());
      serverSpy = spy(server);

      File f = tmp.newFile("existingfile");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      bw.write("herpderp\n");
      bw.close();

      f = tmp.newFile("existingfile.html");
      bw = new BufferedWriter(new FileWriter(f));
      bw.write("<h1>helowworld</h1>\n");
      bw.close();
    }





  /* Check that execute is being called numThreads times */
  @Test
    public void testStartsNumThreads() throws IOException, InterruptedException {

      ExecutorService tpMock = mock(ExecutorService.class);
      serverSpy.threadPool = tpMock;

      (new Thread(serverSpy)).start();
      Thread.sleep(200);
      serverSpy.alive = false;
      verify(tpMock, times(server.numThreads)).execute(any(ThreadPoolCompetingRequestHandler.class));
    }

  @Test
    public void testNewRequestsBlockWhenNoThreadsAvailable() throws IOException, InterruptedException {

      // Simulate a very long request
      when(serverSpy.newThreadPoolCompetingRequestHandler()).thenReturn(
          new ThreadPoolCompetingRequestHandler(serverSpy) {
          @Override
          public void handleRequest() {
          try {
          Thread.sleep(1000);
          } catch (InterruptedException e) { }
          }
          }
          );

      // Simulate ten requests (then no more)
      doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doAnswer(
            new Answer() {
            public Socket answer(InvocationOnMock inv) {
            try {
            Thread.sleep(1000); // simulate blocking after five requests
            } catch (InterruptedException e) {
            System.out.println("interrupted." + e.getMessage());
            }
            return null;
            }
            }
            ).when(serverSpy).acceptIncomingConnection();

      (new Thread(serverSpy)).start();
      Thread.sleep(200);
      serverSpy.alive = false;

      // Because all requests block, we will never accept more than numthreads connections
      verify(serverSpy, times(server.numThreads)).acceptIncomingConnection();
    }

  @Test
    public void testNewRequestsDontBlockWhenThreadsAvailable() throws IOException, InterruptedException {

      // Simulate a very long request
      when(serverSpy.newThreadPoolCompetingRequestHandler()).thenReturn(
          new ThreadPoolCompetingRequestHandler(serverSpy) {
          @Override
          public void handleRequest() {
          try {
          Thread.sleep(1000);
          } catch (InterruptedException e) {

          }
          }
          }
          );

      // Simulate 7 requests (then no more)
      doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doAnswer(
            new Answer() {
            public Socket answer(InvocationOnMock inv) {
            try {
            Thread.sleep(1000); // simulate blocking after five requests
            } catch (InterruptedException e) {
            System.out.println("interrupted." + e.getMessage());
            }
            return null;
            }
            }
            ).when(serverSpy).acceptIncomingConnection();

      (new Thread(serverSpy)).start();
      Thread.sleep(200);
      serverSpy.alive = false;

      // Because all requests block, we will never accept more than numthreads connections
      verify(serverSpy, times(7)).acceptIncomingConnection();
    }



  @Test
    public void testCorrectnes() throws IOException, InterruptedException {


/*
      WebRequest req1 = new WebRequest("existingfile");
      WebRequest req2 = new WebRequest("existingfile.html");
      WebRequest req3 = new WebRequest("nonexistentFile");

      RequestHandler rh1 = new RequestHandler(serverSpy, mockSocket);
      RequestHandler rh1Spy = spy(rh1);

      RequestHandler rh2 = new RequestHandler(serverSpy, mockSocket);
      RequestHandler rh2Spy = spy(rh2);

      RequestHandler rh3 = new RequestHandler(serverSpy, mockSocket);
      RequestHandler rh3Spy = spy(rh3);


      // Simulate a very long request
      when(serverSpy.newThreadPoolCompetingRequestHandler()).thenReturn(
          new ThreadPoolCompetingRequestHandler(serverSpy) {
          @Override
          public void handleRequest() {
          try {
          Thread.sleep(1000);
          } catch (InterruptedException e) {

          }
          }
          }
          );

      // Simulate 7 requests (then no more)
      doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doReturn(mockSocket)
        .doAnswer(
            new Answer() {
            public Socket answer(InvocationOnMock inv) {
            try {
            Thread.sleep(1000); // simulate blocking after five requests
            } catch (InterruptedException e) {
            System.out.println("interrupted." + e.getMessage());
            }
            return null;
            }
            }
            ).when(serverSpy).acceptIncomingConnection();

      (new Thread(serverSpy)).start();
      Thread.sleep(200);
      serverSpy.alive = false;

      // Because all requests block, we will never accept more than numthreads connections
      verify(serverSpy, times(7)).acceptIncomingConnection();
      */
    }
}

