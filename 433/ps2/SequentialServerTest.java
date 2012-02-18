import org.junit.Test;
import org.junit.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;

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
    public void setUp() throws UnknownHostException {

      mockServerSocket = mock(ServerSocket.class);
      mockSocket = mock(Socket.class);

      ss = new SequentialServer();
      ss.serverPort = 333;
      ssSpy = spy(ss);

      cal=  Calendar.getInstance();


    }

  @Test
    public void createFromArgs() {
      // manually tested
    }

  @Test
    public void testReadRequest() {
      try{
        BufferedReader brMock = mock(BufferedReader.class);
        when(brMock.readLine()).thenReturn("GET filedir/filename HTTP/1.0", "Host: yourserver.com", "Accept: text/html", null);
        assertEquals(ss.readRequest(brMock), "GET filedir/filename HTTP/1.0\r\nHost: yourserver.com\r\nAccept: text/html\r\n");
      } catch (Exception e) {e.printStackTrace();}

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

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(SequentialServerTest.class);
  }

}
/*


   private Socket mockSock;
   @Before
   public void setUp() throws UnknownHostException {
   mockSock = mock(Socket.class);
   stc = new SHTTPTestClient(mockSock, 5, "files.txt", 1);
   spyStc = spy(stc);
   }

   @Test
   public void testParseArgs() throws Exception {
   doReturn(new Socket()).when(spyStc).getSocket(anyString(), anyInt());

   stc = SHTTPTestClient.createFromArgs(new String[]{"-server", "localhost", "-port", "12345", "-parallel", "4", "-files", "test.txt", "-T", "10" });

   assertEquals(stc.server, "localhost");
   assertEquals(stc.port, 12345);
   assertEquals(stc.threadCount, 4);
   assertEquals(stc.infile, "test.txt");
   assertEquals(stc.timeout, 10);
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
// ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(stc.threadCount);
ExecutorService executor = mock(ScheduledThreadPoolExecutor.class);
// ScheduledThreadPoolExecutor spyExec = spy(executor);
// when(executor.execute((Runnable) anyObject)).(doNothing);
spyStc.executor = executor;
doReturn(new Socket()).when(spyStc).getSocket(anyString(), anyInt());
doReturn(new GetFileTasks()).when(spyStc).createGetFileTask(any(Socket.class), any(String[].class), anyInt());
try {
spyStc.start();
} catch (ConnectException e)
{
e.printStackTrace();
}
verify(executor, timeout(spyStc.timeout * 1000).times(5)).execute((Runnable) anyObject());
}

@Test
public void testGetFile() throws Exception {
GetFileTasks gft = new GetFileTasks();
GetFileTasks gftSpy = spy(gft);
doNothing().when(gftSpy).writeMessage(anyString());

gftSpy.getFile("file1.txt");
gftSpy.getFile("file2.txt");
gftSpy.getFile("file3.txt");

ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
verify(gftSpy, times(3)).writeMessage(captor.capture());
List<String> args = captor.getAllValues();

assertEquals(gftSpy.requestFileMessage("file1.txt"), args.get(0));
assertEquals(gftSpy.requestFileMessage("file2.txt"), args.get(1));
assertEquals(gftSpy.requestFileMessage("file3.txt"), args.get(2));
}


  @Test
public void testRequestFileMessage()
{
  GetFileTasks gft = new GetFileTasks();

  String urlStr, expectedRequest;
  urlStr = "somegenericstring";
  expectedRequest = "GET " + urlStr + " HTTP/1.0\n\r\n";
  assertEquals(gft.requestFileMessage(urlStr), expectedRequest);

  urlStr = "this//string//has//slashes//";
  expectedRequest = "GET " + urlStr + " HTTP/1.0\n\r\n";
  assertEquals(gft.requestFileMessage(urlStr), expectedRequest);

  urlStr = "this\\string\\has\\\\slashes//";
  expectedRequest = "GET " + urlStr + " HTTP/1.0\n\r\n";
  assertEquals(gft.requestFileMessage(urlStr), expectedRequest);

  urlStr = "&abcABC.*.\\$#%";
  expectedRequest = "GET " + urlStr + " HTTP/1.0\n\r\n";
  assertEquals(gft.requestFileMessage(urlStr), expectedRequest);
}
@Test
public void testNextRequest() {
  try {
    GetFileTasks gft = new GetFileTasks();
    GetFileTasks gftSpy = spy(gft);
    doNothing().when(gftSpy).writeMessage(anyString());

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
    verify(gftSpy, times(7)).getFile(captor.capture());
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
*/
