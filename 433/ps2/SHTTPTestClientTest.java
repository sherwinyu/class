import org.junit.Test;
import org.junit.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;

import org.mockito.stubbing.*;
import org.mockito.invocation.*;
import org.mockito.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import org.mockito.verification.*;


public class SHTTPTestClientTest {


  private SHTTPTestClient stc;
  private SHTTPTestClient spyStc;
  private Socket mockSock;
  @Before
    public void setUp() throws UnknownHostException {
      mockSock = mock(Socket.class);
      stc = new SHTTPTestClient(mockSock, 5, "files.txt", 3);
      spyStc = spy(stc);
      /*
         pc = new PingClient();
         pc.port = 55;
         pc.password = "foo";
         pc.hostAddress = InetAddress.getByName("127.0.0.1");

         pm = new PingMessage();
         long ts = 8388356063466450277L;
         pm.timestamp = ts;
         pm.sequenceNumber = 21326;
         pm.password = pc.password;
         pm.header = "PING";
         */
    }

  // @Test
  public void testParseArgs() throws Exception {
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
      GetFileTasks gft = new GetFileTasks(); //new Socket(InetAddress.getByName("localhost"), 333), new String[]{"file1", "fil2", "fil3"}, 10);
      GetFileTasks gftSpy = spy(gft); //new GetFileTasks(); //new Socket(InetAddress.getByName("localhost"), 333), new String[]{"file1", "fil2", "fil3"}, 10);
      // GetFileTasks gft = new GetFileTasks(new Socket(InetAddress.getByName("localhost"), 333), new String[]{"file1", "fil2", "fil3"}, 10);


      DataOutputStream mockDOS = mock(DataOutputStream.class);
      doNothing().when(gftSpy).writeMessage(anyString());
      // mockDOS.writeBytes("file1.txt");

      // verify(mockDOS).writeBytes(gft.requestFileMessage("file1.txt"));

      gft.dataOutputStream = mockDOS;
      // when(mockDOS).writeBytes(anyString())).doNothing();

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

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(SHTTPTestClientTest.class);
  }


}