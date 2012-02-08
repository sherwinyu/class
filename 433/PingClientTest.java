import org.junit.Test;
import org.junit.*;
import java.net.*;

import static org.junit.Assert.assertEquals;


public class PingClientTest {

  PingClient pc;
  PingMessage pm;

  @Before
    public void setUp() throws UnknownHostException {
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
    }

  @Test
    public void testParseArgs() throws Exception {
      PingClient t = new PingClient();
      t.parseArgs(new String[]{"127.0.0.1", "555", "foobar"}); 
      assertEquals(t.hostAddress.getCanonicalHostName(),
          InetAddress.getByName("127.0.0.1").getCanonicalHostName() );
    }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(PingClientTest.class);
  }


}

