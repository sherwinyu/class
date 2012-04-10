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
    aa(this, sock.getTCPManager() == this, "adding socket that isn't owned by me");
    aa(this, sock.isWelcome(), "adding non-welcome socket");
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
    TCPSock sock = getSockByTSID(tsid);
    p(sock, "sending SYN " + tsid.id());
    ppt("S");
    send(tsid, Transport.SYN, getSockByTSID(tsid).getTransportSeqNum(), new byte[]{});
  }

  public void sendACK(TCPSockID tsid) {
    TCPSock recvSock = getSockByTSID(tsid);
    p(recvSock, 3, "sending ACK. ackNum: " + recvSock.getRecvBase() + tsid.id());
    ppt("A");
    send(tsid, Transport.ACK, recvSock.getRecvBase(), new byte[]{});
  }

  public void sendFIN(TCPSockID tsid) {
    TCPSock sock = getSockByTSID(tsid);
    if (sock != null)
      p(sock, "sending FIN " + tsid.id());
    ppt("F");
    //send(tsid, Transport.FIN, 0, new byte[]{});

    Transport t = new Transport(tsid.getLocalPort(), tsid.getRemotePort(),
        Transport.FIN, 0, 0, new byte[]{});
    send(tsid, t);
    // node.sendSegment(tsid.getLocalAddr(), tsid.getRemoteAddr(), Protocol.TRANSPORT_PKT, t.pack());
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
    TCPSock sock = getSockByTSID(tsid);
    p(sock, 2, "sending data: " + transportToString(t));
    p(sock, 2, "sending datad: seqNum (" + t.getSeqNum() + ") sendBase (" + sock.getSendBase() + ")");

    // only permit outgoing packets with sn >= sb
    // because sb is last ack that was received (meaning receiver is expecting
    // packet with sequence number == sb)
    if (t.getSeqNum() >= sock.getSendBase())  {
      ppt(t.getSeqNum() == sock.getTransportSeqNum() ? "." : "!");
      send(tsid, t);
      p(sock, 3, "timer added");
      node.addSendDataTimer(expectedRTT(tsid), tsid, t);
    }
    else 
      p(sock, 5, "packet not sent because ack received: seqNum (" + t.getSeqNum() + ") >= sendBase (" + sock.getSendBase() + ")");
  }

  /**
   * @return the expected RTT for this connection
   */
  private int expectedRTT(TCPSockID tsid) {
    return 2000;
  }

  public static String bytesToString(byte[] arr) {
    String out = "";
    int preview = 3;

    for (int i = 0; i < Math.min(preview, arr.length); i++)
      out += (int) arr[i] + " ";

    out += "... ";

    for (int i = 0; i < Math.min(preview, arr.length); i++) 
      out += (int) arr[arr.length + i - Math.min(preview, arr.length)] + " ";
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
  public boolean handleSYN(TCPSockID tsid, Transport t) {
    if (welcomeSocks.containsKey(tsid.getLocalPort())) {
      p(this, 3, "syn matched on localPort: " + tsid.getLocalPort());
      TCPSock welcomeSock = welcomeSocks.get(tsid.getLocalPort());
      TCPSock recvSock = socket(SocketType.RECEIVER);
      recvSock.setTSID(tsid);
      welcomeSock.addRecievingSock(recvSock);
      return true;
    }
    else
      return false;
  }

  public boolean handleACK(TCPSockID tsid, Transport t) {
    TCPSock sock = getSockByTSID(tsid);
    if (sock == null)
      return false;
    aa(this, sock.isSender(), "ack received by non sender socket");
    aa(this, sock.isConnectionPending() || sock.isConnected() || sock.isClosurePending(), "ack received by invalid socket state");

    if (sock.isConnectionPending()) {
      p(this, 3, "synAck received");
      sock.synAckReceived();
      return true;
    } 
    else if (sock.isConnected() || sock.isClosurePending()) {
      p(this, 3, "dataAck received... doing nothing for now");
      sock.dataAckReceived(t);
      return true;
    }
    return false;
  }

  public boolean handleDATA(TCPSockID tsid, Transport t) {
    TCPSock sock = getSockByTSID(tsid);
    if (sock == null)
      return false;

    if (!sock.isConnected())
      return false;

    aa(this, sock.isReceiver(), "DATA received by non receiver socket");
    aa(this, sock.isConnected(),  "DATA received by invalid socket state");
    sock.dataReceived(t);
    return true;
  }

  public boolean handleFIN(TCPSockID tsid, Transport t) {
    TCPSock sock = getSockByTSID(tsid);
    if (sock != null) {
      p(sock, 3, "FIN -> releasing"); 
      sock.release();
    }
    return true;
  }

  /* * End Socket API */

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

}
