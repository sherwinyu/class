import org.junit.Test;
import org.junit.*;
import java.net.*;

import static org.junit.Assert.assertEquals;


public class SHTTPTestClientTest {


  @Before
    public void setUp() throws UnknownHostException {
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

  @Test
    public void testParseArgs() throws Exception {
      SHTTPTestClient stc = SHTTPTestClient.createFromArgs(new String[]{"-server", "localhost", "-port", "12345", "-parallel", "4", "-files", "test.txt", "-T", "10" });
      assertEquals(stc.server, "localhost");
      assertEquals(stc.port, 12345);
      assertEquals(stc.threadCount, 4);
      assertEquals(stc.infile, "test.txt");
      assertEquals(stc.timeout, 10);
    }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(SHTTPTestClientTest.class);
  }


}

