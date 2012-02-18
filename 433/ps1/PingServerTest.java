import org.junit.Test;
import org.junit.*;
import java.net.*;
import java.nio.ByteBuffer;
import org.mockito.stubbing.*;
import org.mockito.invocation.*;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class PingServerTest {

  PingMessage pm;
  PingClient pc;
  PingServer ps;

  @Before
    public void setUp() throws UnknownHostException {
      /*
         pc = new PingClient();
         pc.port = 55;
         pc.password = "foo";
         pc.hostAddress = InetAddress.getByName("127.0.0.1");
         */

      ps = new PingServer();
      ps.serverPassword = "foo";
      ps.port = 33333;
      ps.LOSS_RATE = 0.0;
      ps.AVERAGE_DELAY = 25;


      pm = new PingMessage();
      long ts = 8388356063466450277L;
      pm.timestamp = ts;
      pm.sequenceNumber = 21326;
      pm.password = ps.serverPassword;
      pm.header = "PING";

    }

  @Test
    public void testParseArgsPortAndPassOnly() {
      PingServer.parseArgs(new String[]{"12345", "foobar"});
      assertEquals("Port assigned to", 12345, PingServer.port_arg);
      assertEquals("Password assigned to", "foobar", PingServer.password_arg);
    }
  @Test
    public void testParseArgsWithDelay() {
      PingServer.parseArgs(new String[]{"12345", "foobar", "150"});
      assertEquals("Port assigned to", 12345, PingServer.port_arg);
      assertEquals("Password assigned to", "foobar", PingServer.password_arg);
      assertEquals("DELAY assigned to", 150, PingServer.delay_arg);
    }

  @Test
    public void testParseArgsWithDelayAndLossRate() {
      PingServer.parseArgs(new String[]{"12345", "foobar", "150", "0.75"});
      assertEquals("Port assigned to", 12345, PingServer.port_arg);
      assertEquals("Password assigned to", "foobar", PingServer.password_arg);
      assertEquals("AVERAGE_DELAY assigned to", 150, PingServer.delay_arg);
      assertEquals("LOSS_RATE assigned to", 0.75, PingServer.lossrate_arg, 0.0001);
    }

  @Test
    public void testPingMessageUnpackBytes() {
      pm.timestamp =  System.currentTimeMillis();
      byte[] arr = pm.toByteArr();
      PingMessage msg = PingMessage.unpackBytes(arr);
      assertEquals(msg.sequenceNumber, pm.sequenceNumber);
      assertEquals(msg.timestamp, pm.timestamp);
      assertEquals(msg.header, "PING");
    }

  @Test
    public void testPingMessageToByteArray() {
      long ts = 8388356063466450277L;
      String ans = "PINGSNtimetimefoo\r\n";
      assertEquals(new String(pm.toByteArr()), ans);
      assertEquals(new String(pm.toByteArr()), ans);
      assertEquals("byteArr length", pm.toByteArr().length, 19);

      pm.header = "PINGECHO";
      ans = "PINGECHOSNtimetimefoo\r\n";
      assertEquals(new String(pm.toByteArr()), ans);
      assertEquals("byteArr length", pm.toByteArr().length, 23);
    }
   @Test
    public void shouldIgnoreWrongPassword() throws Exception {
      /*
      ps.setSocket( new DatagramSocket() {
          public void receive(DatagramPacket response) throws IOException {
            pm.password = "wrongpassword";
            response.setData(pm.toByteArr());
            response.setPort(555);
            response.setLength(pm.toByteArr().length);
            response.setAddress(InetAddress.getByName("127.0.0.1"));
          }
          public int getLocalPort() { return 1337;}
        });
        */

      DatagramSocket mockSocket = mock(DatagramSocket.class);
      ps.setSocket(mockSocket);
      doAnswer( new Answer<Void>() {
          public Void answer(InvocationOnMock invocation) {
          DatagramPacket response = (DatagramPacket) invocation.getArguments()[0];
          pm.password = ps.serverPassword + "wrong";
          response.setData(pm.toByteArr());
          response.setPort(555);
          response.setLength(pm.toByteArr().length);
          try {
          response.setAddress(InetAddress.getByName("127.0.0.1"));
          } catch (Exception e) { ; }
          return null;
          }
          }).when(mockSocket).receive((DatagramPacket) anyObject());

      ps.start();
      verifyNoMoreInteractions(mockSocket);
      Thread.sleep(50);
      ps.stopRunning();


    }

   @Test
    public void replyWhenPasswordCorrect() throws Exception {
      DatagramSocket mockSocket = mock(DatagramSocket.class);
      ps.serverPassword = "foo";
      ps.setSocket(mockSocket);
      doAnswer( new Answer<Void>() {
          public Void answer(InvocationOnMock invocation) {
          DatagramPacket response = (DatagramPacket) invocation.getArguments()[0];
          pm.password = ps.serverPassword;
          response.setData(pm.toByteArr());
          response.setPort(555);
          response.setLength(pm.toByteArr().length);
          try {
          response.setAddress(InetAddress.getByName("127.0.0.1"));
          } catch (Exception e) { e.printStackTrace(); }
          return null;
          }
          }).when(mockSocket).receive((DatagramPacket) anyObject());

      ps.start();
      verify(mockSocket, timeout(300).atLeastOnce()).send((DatagramPacket) anyObject());
      Thread.sleep(50);
      ps.stopRunning();
    }


  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(PingServerTest.class);
  }

}

