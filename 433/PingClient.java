import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;

public class PingClient
{

  InetAddress hostAddress;
  int port;
  short sequenceNumber = 0;
  String password = " ";
  DatagramSocket clientSocket;

  public void parseArgs(String[] args) {
    try {
    if (args.length != 3)
      throw new NumberFormatException("improper commandline arguments");
      hostAddress = InetAddress.getByName(args[0]);
      port = Integer.parseInt(args[1]);
      password = args[2];
    }
    catch (NumberFormatException e) {
      System.out.println("Usage: java PingClient <server address> <port> <server password>");
      System.exit(0);
    }
    catch (UnknownHostException e) {
      System.out.println("Host unknown.");
      System.exit(0);
    }

  }

  public void ping (short sequenceNumber) throws IOException
  {
    PingMessage pm = new PingMessage();
    pm.header = "PING";
    pm.sequenceNumber = sequenceNumber;
    pm.timestamp = System.currentTimeMillis();
    pm.password = password;
    byte[] sendData = pm.toByteArr();
    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, hostAddress, port);
    clientSocket.send(sendPacket);
  }

  public ArrayList<Long> pingTenTimes(int timeout) throws SocketException {
    final Timer timer = new Timer();
    clientSocket.setSoTimeout(timeout);
    sequenceNumber = 0;
    final ArrayList<Long> RTTs = new ArrayList<Long>();
    timer.scheduleAtFixedRate( new TimerTask() {
          public void run() {

            if (sequenceNumber == 10) {
              this.cancel();
              timer.cancel();
              printStats(RTTs);
              System.out.println("Terminating.");
              return;
            }
            try {
              long ts = System.currentTimeMillis();
              ping(sequenceNumber);
              try {
                DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                clientSocket.receive(response);
                PingMessage pm = PingMessage.unpackEchoBytes(response.getData());
                long delta = System.currentTimeMillis() - ts;
                RTTs.add(delta);
                System.out.println("Response received. RTT: " + delta + " Response: " + pm.getString());
              }
              catch (SocketTimeoutException e) {
                System.out.println("Response timed out. Continuing...");
              }

            }
            catch (IOException e) {
              e.printStackTrace();
            }

            sequenceNumber++;
          }}, 0, 1000);
    return RTTs;
  }

  public void printStats(ArrayList<Long> RTTs)
  {
    long sum = 0;
    long maxRTT = 0;
    long minRTT  = 1000000000000L;
    for (long l : RTTs) {
      sum += l;
      if (l > maxRTT)
        maxRTT = l;
      if (l < minRTT)
        minRTT = l;
    }
    System.out.println("Number of replies:" + RTTs.size());
    System.out.println("Min RTT:" + minRTT);
    System.out.println("Max RTT:" + maxRTT);
    System.out.println("Avg RTT:" + (double) sum / RTTs.size());
  }

  public static void main(String[] args) throws Exception {

    //TODO(syu) code for handling differing arguments
    PingClient pc = new PingClient();
    pc.parseArgs(args);
    pc.clientSocket = new DatagramSocket();
    pc.pingTenTimes(1000);


  } // end of main
}
