import java.util.*;
import syu.*;
import static syu.SU.*;

/**
 * <p>Title: CPSC 433/533 Programming Assignment</p>
 *
 * <p>Description: Fishnet TCP manager</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Yale University</p>
 *
 * @author Hao Wang
 * @version 1.0
 */
public class TCPManager implements Debuggable {
  public String id() {
    return "TCPMgr#"+node.getAddr();
  }
  Node node;
  private int addr;
  private int packetSeqNum;
  private Manager manager;

  private static final byte dummy[] = new byte[0];
  HashMap<TCPSockID, TCPSock> sockSpace;
  HashMap<Integer, TCPSock> welcomeSocks;

  public TCPManager(Node node, int addr, Manager manager) {
    this.node = node;
    this.addr = addr;
    this.packetSeqNum = 0;

    this.manager = manager;
    this.sockSpace = new HashMap<TCPSockID, TCPSock>();
    this.welcomeSocks = new HashMap<Integer, TCPSock>();
  }

  // public send

  /**
   * Start this TCP manager
   */
  public void start() {

  }

  /*
   * Begin socket API
   */

  /**
   * Create a socket
   *
   * @return TCPSock the newly created socket, which is not yet bound to
   *                 a local port
   */
  public TCPSock socket(SocketType type) {
    // return null;
    return new TCPSock(this, type);
  }

  public void add(TCPSockID tsid, TCPSock sock) {
    if (tsid.localAddr != this.addr)
      die(this, "adding tsid with nonlocal address");
    if (sockSpace.containsKey(tsid)) {
      p(this, "in add(). Conflict detected! Tsid already in map: " + tsid);
    }
    sockSpace.put(tsid, sock);
  }

  public void addWelcomeSocket(TCPSock sock) {
    if (sock.getTCPManager() != this) 
      die(this, "adding socket that isn't owned by me");
    if (sock.getSockType() != SocketType.WELCOME) 
      die(this, "adding non-welcome socket");
    welcomeSocks.put(sock.getLocalPort(), sock);
    // add(new TCPSockID(addr, -1, sock.getLocalPort(), -1), sock);
  }

  // tsid is a a properly oriented tsid wrt this socket.

  /**
   *  handled an incoming SYN packet. 
   *  
   *  @param tsid Tuple oriented relative to local sockets
   *  @param t Transport packet containing some information.
   *
   *  1) look up corresponding welcome socket
   *  2) add appropriate receiving socket to queue, with 4tuple set
   */
  public void sendSYN(TCPSockID tsid) {
    p(this, "sending SYN " + tsid.id());
    send(tsid, Transport.SYN, new byte[]{});
  }
  public void sendACK(TCPSockID tsid) {
    p(this, "sending ACK " + tsid.id());
    send(tsid, Transport.ACK, new byte[]{});
  }

  public void send(TCPSockID tsid, int type, byte[] payload) { 
    // p(tsid, "is...?");
    ppp("\nkeys:");
    ppp("" + sockSpace.keySet());
    aa(this, sockSpace.containsKey(tsid), "attempting to send with nonexistent tsid");
    send(tsid, type, sockSpace.get(tsid).getTransportSeqNum(), payload);
  } 

  public void send(TCPSockID tsid, int type, int seqNum, byte[] payload) {
    Transport t = getSockByTSID(tsid).makeTransport(type, seqNum, payload);
    send(tsid, t);
  }

  public void send(TCPSockID tsid, Transport t) {
    node.sendSegment(tsid.getLocalAddr(), tsid.getRemoteAddr(), Protocol.TRANSPORT_PKT, t.pack());
  }

  /**
   * Sends a data packet to remoteAddr.
   *
   * @param  remoteAddr destination address of packet
   * @param lastSeqNum the last transport sequence number of the TCP transport contained in the packet 
   * @param p the Packet to be delivered to node.send()
   *
   * Only delivered if sendBase is less than seqNum, otherwise the to-be-sent packet has already been acked.
   * sendBase is the seqnum of the last ACK received
   * so for a packet to be sent, it's seqNum must be >= sendBase
   */
  public void sendData(TCPSockID tsid, Transport t) {
    aa(this, sockSpace.containsKey(tsid), "attempting to send with nonexistent tsid");

    TCPSock sock = getSockByTSID(tsid);
    p(sock, 2, "sending data: " + transportToString(t));

    if (t.getSeqNum() >= sock.getSendBase())  {
      node.sendSegment(tsid.getLocalAddr(), tsid.getRemoteAddr(),
          Protocol.TRANSPORT_PKT, t.pack());
    }
    else 
      p(sock, 3, "packet not sent because ack received: seqNum (" + t.getSeqNum() + ") >= sendBase (" + sock.getSendBase() + ")");
  }

  public static String bytesToString(byte[] arr) {
    String out = "";
    int preview = 5;

    for (int i = 0; i < Math.min(preview, arr.length); i++)
      out += (int) arr[i] + " ";

    out += "... ";

    for (int i = 0; i < Math.min(preview, arr.length); i++) 
      out += (int) arr[arr.length - i - 1] + " ";
    return out;
  }

  public static String transportToString(Transport t) {
    return bytesToString(t.getPayload());
  }



  /**
   * Handle an incoming SYN
   * @param tsid TSID oriented relative to local sockets
   * @param t is this necessary? TODO(syu)
   *
   * Check if incoming SYN matches a welcome socket
   * if so, create new recvSock
   * add recvSock to queue of the welcomeSock
   */
  public void handleSYN(TCPSockID tsid, Transport t) {
    if (welcomeSocks.containsKey(tsid.getLocalPort())) {
      p(this, 3, "local welcome port matched!");
      TCPSock sock = welcomeSocks.get(tsid.getLocalPort());
      TCPSock recvSock = socket(SocketType.RECEIVER);
      recvSock.setTSID(tsid);
      sock.addRecievingSock(recvSock);
    }
    else
      p(this, 3, "rejecting SYN");
  }

  public void handleACK(TCPSockID tsid) {
    TCPSock sock = getSockByTSID(tsid);
    aa(this, sock.getSockType() == SocketType.SENDER, "ack received by non sender socket");
    aa(this, sock.isConnectionPending() || sock.isConnected(), "ack received by invalid socket state");

    if (sock.isConnectionPending()) {
      p(this, "sock is pending connection");
      sock.synAckReceived();
    } 
    else if (sock.isConnected()) {
      p(this, "sock is established");
      sock.synAckReceived();
    }

  }

  /*
   * End Socket API
   */

  public int getAddr() {
    return this.node.getAddr();
  }

  public TCPSock getSockByTSID(TCPSockID tsid) {
    return sockSpace.get(tsid);
  }

  //TODO(syu): implement this
  public boolean isPortFree(int localPort) {
    return true;
  }

  public int getPacketSeqNum() {
    return packetSeqNum++;
  }

}
