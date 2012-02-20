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

import syu.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.verification.*;


public class SHTTPTestClientTest {


  private SHTTPTestClient stc;
  private SHTTPTestClient spyStc;
  private Socket mockSock;
  private InetSocketAddress mockAddr;
  @Before
    public void setUp() throws UnknownHostException {
      mockSock = mock(Socket.class);
      mockAddr = mock(InetSocketAddress.class);
      stc = new SHTTPTestClient(mockAddr, 5, "files.txt", 1);
      spyStc = spy(stc);
    }

  @Test
    public void testParseArgs() throws Exception {
      // doReturn(new Socket()).when(spyStc).getSocket(anyString(), anyInt());
      stc = SHTTPTestClient.createFromArgs(new String[]{"-server", "localhost", "-port", "12345", "-parallel", "4", "-files", "files.txt", "-T", "10" });
      assertEquals("server", stc.server, "localhost");
      assertEquals("port", stc.port, 12345);
      assertEquals("thread count", 4, stc.threadCount);
      assertEquals("inputfile", stc.infile, "files.txt");
      assertArrayEquals("filenames", stc.filenames, new String[]{"a.txt","bbbbb", "ccccc", "d.jpg"});
      assertEquals("timeout", 10,  stc.timeout);
    }

  protected void implementAsDirectExecutor(Executor executor) {
    doAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation)
        throws Exception {
        Object[] args = invocation.getArguments();
        Runnable runnable = (Runnable)args[0];
        runnable.run();
        return null;
        }
        }).when(executor).execute(any(Runnable.class));
  }

  @Test
    public void testStart() throws Exception {
      ExecutorService executor = mock(ScheduledThreadPoolExecutor.class);
      spyStc.executor = executor;
      doReturn(new GetFileTasks()).when(spyStc).createGetFileTask(any(InetSocketAddress.class), any(String[].class), anyInt());
      // try {
        spyStc.start();
      // } catch (ConnectException e) { e.printStackTrace(); }
      verify(executor, timeout(spyStc.timeout * 1000).times(5)).execute((Runnable) anyObject());
    }

  @Test
  @SuppressWarnings("unchecked")
    public void testProcessFile() throws Exception {
      GetFileTasks gft = new GetFileTasks();
      GetFileTasks gftSpy = spy(gft);

      ArrayList<Integer> sizesMock = (ArrayList<Integer>) mock(ArrayList.class);
      ArrayList<Integer> delaysMock = (ArrayList<Integer>) mock(ArrayList.class);
      gft.fileSizes = sizesMock;
      gft.delays = delaysMock;


      doNothing().when(gftSpy).writeMessage(anyString());

      doAnswer( new Answer() {
          public String answer(InvocationOnMock inv) {
            return  WebResponse.okResponse("servername", "text/plain", 9, "herpderp\n".getBytes()).toString();
          }
      } ).when(gftSpy).receiveResponse();

      gftSpy.processFile("file1.txt");
      gftSpy.processFile("file2.txt");
      gftSpy.processFile("file3.txt");

      System.out.println(sizesMock.size() + "\t " + gft.fileSizes.size(  ));
      System.out.println(delaysMock.size() +"\t " + gft.delays.size() );

      ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
      verify(gftSpy, times(3)).writeMessage(captor.capture());
      verify(gftSpy, times(3)).collectStats(anyInt(), anyInt());
      // verify(sizesMock, times(3)).add(anyInt());
      // verify(delaysMock, times(3)).add(anyInt());


      List<String> args = captor.getAllValues();

      assertEquals(gftSpy.requestFileMessage("file1.txt"), args.get(0));
      assertEquals(gftSpy.requestFileMessage("file2.txt"), args.get(1));
      assertEquals(gftSpy.requestFileMessage("file3.txt"), args.get(2));

    }

  /*
     GET <URL> HTTP/1.0
     CRLF
     */

  @Test
    public void testRequestFileMessage()
    {
      GetFileTasks gft = new GetFileTasks();

      String urlStr, expectedRequest;
      urlStr = "somegenericstring";
      expectedRequest = "GET " + urlStr + " HTTP/1.0\r\n\r\n";
      assertEquals(gft.requestFileMessage(urlStr), expectedRequest);

      urlStr = "this//string//has//slashes//";
      expectedRequest = "GET " + urlStr + " HTTP/1.0\r\n\r\n";
      assertEquals(gft.requestFileMessage(urlStr), expectedRequest);

      urlStr = "this\\string\\has\\\\slashes//";
      expectedRequest = "GET " + urlStr + " HTTP/1.0\r\n\r\n";
      assertEquals(gft.requestFileMessage(urlStr), expectedRequest);

      urlStr = "&abcABC.*.\\$#%";
      expectedRequest = "GET " + urlStr + " HTTP/1.0\r\n\r\n";
      assertEquals(gft.requestFileMessage(urlStr), expectedRequest);
    }

  @Test
    public void testNextRequest() {
      try {
      GetFileTasks gft = new GetFileTasks();
      GetFileTasks gftSpy = spy(gft);
      doNothing().when(gftSpy).writeMessage(anyString());
      doReturn("something").when(gftSpy).receiveResponse();

      String[] filenames = {"abc#def", "voice/b/0?pli=1#inbox", "test..test..test", "//\\/"};
      gftSpy.filenames = filenames;
        gftSpy.nextRequest();
        gftSpy.nextRequest();
        gftSpy.nextRequest();
        gftSpy.nextRequest();
        gftSpy.nextRequest();
        gftSpy.nextRequest();
        gftSpy.nextRequest();

      ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
      verify(gftSpy, times(7)).processFile(captor.capture());
      List<String> args = captor.getAllValues();

      assertEquals(filenames[0], args.get(0));
      assertEquals(filenames[1], args.get(1));
      assertEquals(filenames[2], args.get(2));
      assertEquals(filenames[3], args.get(3));
      assertEquals(filenames[0], args.get(4));
      assertEquals(filenames[1], args.get(5));
      assertEquals(filenames[2], args.get(6));
      } catch (IOException e) {e.printStackTrace();}
    }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(SHTTPTestClientTest.class);
  }


}
