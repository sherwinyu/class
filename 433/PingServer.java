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

  public PingMessage() {
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

        byte[] buf = request.getData();
        PingMessage msg = new PingMessage(buf, false);

        System.out.println("Received request: " + msg.getString());

        // Simulate packet loss
        if (random.nextDouble() < LOSS_RATE) {
          System.out.println("   ...but simulating packet loss.");
          continue;
        }
        Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

        InetAddress clientHost = request.getAddress();
        int clientPort = request.getPort();
        if (!msg.password.equals(serverPassword))
        {
          System.out.println("     ...but reply not sent: password missmatch");
          continue;
        }

        msg.header = "PINGECHO";
        buf = msg.toByteArr();
        reply(msg, request.getAddress(), request.getPort());
        System.out.println("    Reply sent: " + msg.getString());

      } while (running);

    } catch (Exception e) { e.printStackTrace(); }
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
}
