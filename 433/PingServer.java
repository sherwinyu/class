// PingServer.java

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;

/*
 * Server to process ping requests over UDP.
 */
class PingMessage {
  String header;
  short sequenceNumber;
  long timestamp;
  String password;

  public String getString() {
    return new StringBuffer().append(header)
      .append(" ")
      .append(sequenceNumber)
      .append(" ")
      .append(timestamp)
      .append(" ")
      .append(password).toString();
  }
  public byte[] toByteArr() {
    ByteBuffer sb = ByteBuffer.allocate(this.header.length() + 12 + this.password.length());
    sb.put(this.header.getBytes());
    sb.putShort(this.sequenceNumber);
    sb.putLong(this.timestamp).put( (this.password+"\r\n").getBytes());
    return sb.array();
  }

  public PingMessage()
  {
    this.header = "";
  }
  public PingMessage(byte[] in, boolean isEcho)
  {
    int headerLength = isEcho ? 8 : 4;
    ByteBuffer bb = ByteBuffer.wrap(in);
    byte[] arr  = new byte[headerLength];
    bb.get(arr, 0, headerLength);
    this.header = new String(arr);

    this. sequenceNumber = bb.getShort();
    this.timestamp = bb.getLong();

    arr = new byte[in.length - bb.position()];
    bb.get(arr, 0, arr.length);
    String s = new String(arr);
    this.password = s.substring(0, s.indexOf("\r"));
  }

  public static PingMessage unpackBytes(byte[] in) {
    return new PingMessage(in, false);
  }

  public static PingMessage unpackEchoBytes(byte[] in) {
    return new PingMessage(in, true);
  }

}

public class PingServer extends Thread
{

  double LOSS_RATE = 0.3;
  int AVERAGE_DELAY = 100;  // milliseconds
  String serverPassword = "";
  int port;

  private DatagramSocket socket;
  private boolean running = true;

  public void stopRunning()
  {
    running = false;
  }

  public void run() 
  {

    Random random = new Random();
    running = true;
    try {
      do {
        DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
        socket.receive(request);
        printData(request);

        // Decide whether to reply, or simulate packet loss.
        if (random.nextDouble() < LOSS_RATE) {
          System.out.println("   Packet loss.");
          continue;
        }
        Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

        InetAddress clientHost = request.getAddress();
        int clientPort = request.getPort();

        byte[] buf = request.getData();
        PingMessage msg = new PingMessage(buf, false);
        if (!msg.password.equals(serverPassword))
        {
          System.out.println("     Reply not sent: password missmatch");
          continue;
        }

        msg.header = "PINGECHO";
        buf = msg.toByteArr();
        reply(msg, request.getAddress(), request.getPort());
        System.out.println("   Reply sent: " + msg.getString());

      } while (running);

    } catch (Exception e) { e.printStackTrace(); }
  }
  void receive(DatagramPacket request) {


  }
  void reply(PingMessage pm, InetAddress clientHost, int clientPort) throws IOException {
    DatagramPacket reply = new DatagramPacket(
        pm.toByteArr(),
        pm.toByteArr().length,
        clientHost,
        clientPort);
    socket.send(reply);
  }

  public void setSocket(DatagramSocket s) {
    this.socket = s;
  }

  static int port_arg;
  static String password_arg;
  static int delay_arg;
  static double lossrate_arg;

  public static void parseArgs(String[] args) {

    try {
      port_arg = Integer.parseInt(args[0]);
      password_arg = args[1];


      if (args.length >= 3)
        delay_arg = Integer.parseInt(args[2]);

      if (args.length >= 4)
        lossrate_arg = Double.parseDouble(args[3]);
      if (args.length >= 5) throw new Exception("improper commandline arguments");

    } catch (Exception e) {
      System.out.println("Usage: java PingServer <port> <password> [<delay> <lossrate>]");
      System.exit(0);
    }
  }
  public static void main(String[] args) throws Exception
  {
    parseArgs(args);

    PingServer ps = new PingServer();

    ps.port = port_arg;
    ps.serverPassword = password_arg;
    ps.AVERAGE_DELAY = delay_arg;
    ps.LOSS_RATE = lossrate_arg;

    ps.setSocket(new DatagramSocket(ps.port));

    ps.run();
  }


  /*
   * Print ping data to the standard output stream.
   */
  private void printData(DatagramPacket request) throws Exception
  {
    // Obtain references to the packet's array of bytes.
    byte[] buf = request.getData();

    // Wrap the bytes in a byte array input stream,
    // so that you can read the data as a stream of bytes.
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);

    // Wrap the byte array output stream in an input stream reader,
    // so you can read the data as a stream of **characters**: reader/writer handles characters
    InputStreamReader isr = new InputStreamReader(bais);

    // Wrap the input stream reader in a bufferred reader,
    // so you can read the character data a line at a time.
    // (A line is a sequence of chars terminated by any combination of \r and \n.)
    BufferedReader br = new BufferedReader(isr);

    // The message data is contained in a single line, so read this line.
    String line = br.readLine();

    // Print host address and data received from it.
    System.out.println(
        "Received from " +
        request.getAddress().getHostAddress() +
        ": " +
        new String(line) );
  }
}
