import java.util.*;
import java.nio.*;

import syu.*;
import static syu.SU.*;
/**
 * <p>Title: CPSC 433/533 Programming Assignment</p>
 *
 * <p>Description: Fishnet socket implementation</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Yale University</p>
 *
 * @author Hao Wang
 * @version 1.0
 */

enum SocketType {
  WELCOME,
  SENDER,
  RECEIVER
}
public class TCPSock implements Debuggable {
  public String id() {
    return sockType + "(" + state + "  " + tsid.getLocalAddr() +":" + getLocalPort() + ")";
  }
  // TCP socket states
  enum State {
    // protocol states
    CLOSED,
    LISTEN,
    SYN_SENT,
    ESTABLISHED,
    SHUTDOWN // close requested, FIN not sent (due to unsent data in queue)
  }

  public int SEND_BUFFER_SIZE = 1000;

  // All sockets
  private State state;
  private SocketType sockType;
  private TCPManager tcpMan;
  private TCPSockID tsid;


  // Send sockets
  private int sendWindow;
  private int sendBase;
  private int seqNum;
  // private HashMap<Integer, Boolean> outgoingPacketStatus;
  private ByteBuffer outbb;

  // Receive sockets
  private int recvBase;

  // WelcomeSocket
  private int backlog;
  private ArrayList<TCPSock> welcomeQueue;

  public TCPSock(TCPManager tcpMan, SocketType type) {
    this.tcpMan = tcpMan;
    this.tsid = new TCPSockID();
    this.tsid.localAddr = tcpMan.getAddr();
    this.state = State.CLOSED;
    this.sockType = type;

    switch (sockType) {
      case SENDER:
        initSender();
        break;
      case RECEIVER:
        initReceiver();
        break;
      case WELCOME:
        initWelcome();
        break;

    }

    this.seqNum = 0;

  }

  private void initSender() {
    aa(this, sockType == SocketType.SENDER, "");
    sendWindow = 10 * Transport.MAX_PAYLOAD_SIZE;
    sendBase = 0;
    seqNum = 0;
    outbb = ByteBuffer.allocate(SEND_BUFFER_SIZE);
  }

  private void initReceiver() {
    aa(this, sockType == SocketType.RECEIVER, "");
    recvBase = 0;
  }

  private void initWelcome() {
    aa(this, sockType == SocketType.WELCOME, "");
    this.welcomeQueue = null;
    this.backlog = -1;
  }

  /*
   * The following are the socket APIs of TCP transport service.
   * All APIs are NON-BLOCKING.
   */

  /**
   * Bind a socket to a local port
   *
   * @param localPort int local port number to bind the socket to
   * @return int 0 on success, -1 otherwise
   */
  public int bind(int localPort) {
    // TODO(syu): check if local port in use
    if (tcpMan.isPortFree(localPort)) {
      this.tsid.localPort = localPort;
      return 0;
    }
    return -1;
  }

  /**
   * Listen for connections on a socket
   * @param backlog int Maximum number of pending connections
   * @return int 0 on success, -1 otherwise
   */
  public int listen(int backlog) {

    if (sockType != SocketType.WELCOME) {
      die(this, "not a welcome socket");
      return -1;
    }

    this.backlog = backlog;
    welcomeQueue = new ArrayList<TCPSock>(backlog);
    this.state = State.LISTEN;
    tcpMan.addWelcomeSocket(this);
    return 0;

  }

  /**
   * Accept a connection on a socket
   *
   * Needs to trigger sending an ACK
   *
   * @return TCPSock The first established connection on the request queue
   * remove from welcomeQueue
   * adds recvSocket to socketSpace
   * sends Ack
   */
  public TCPSock accept() {
    aa(this, this.sockType == SocketType.WELCOME, "accept() called by non-welcome socket");
    aa(this, this.state == State.LISTEN, "accept() called when not listening");

    if (welcomeQueue.isEmpty())
      return null;

    TCPSock recvSock = welcomeQueue.remove(0);
    tcpMan.add(recvSock.tsid, recvSock);
    tcpMan.sendACK(recvSock.tsid);

    return recvSock;
  }

  public boolean isConnectionPending() {
    return (state == State.SYN_SENT);
  }

  public boolean isClosed() {
    return (state == State.CLOSED);
  }

  public boolean isConnected() {
    return (state == State.ESTABLISHED);
  }

  public boolean isClosurePending() {
    return (state == State.SHUTDOWN);
  }

  /**
   * Initiate connection to a remote socket
   *
   * @param destAddr int Destination node address
   * @param destPort int Destination port
   * @return int 0 on success, -1 otherwise
   */
  public int connect(int destAddr, int destPort) {
    // if not starting from a closed state, connect should fail.
    if (!isClosed() || sockType != SocketType.SENDER) {
      pe(this, " connect() called on inappropriated");
      return -1;
    }
    this.tsid.remoteAddr = destAddr;
    this.tsid.remotePort = destPort;

    tcpMan.add(this.tsid, this);
    tcpMan.sendSYN(this.tsid);
    this.state = State.SYN_SENT;

    return 0;
    // int window = 5;
    // int ttl = 5;
    // Transport t = new Transport(getLocalPort(), destPort, Transport.SYN, window, this.seqNum, new byte[]{});
    // Packet p = new Packet(destAddr, tcpMan.getAddr(), ttl, Protocol.TRANSPORT_PKT, tcpMan.getPacketSeqNum(), t.pack());
    // tcpMan.node.send(destAddr, p);


  }

  /**
   * Initiate closure of a connection (graceful shutdown)
   */
  public void close() {
  }

  /**
   * Release a connection immediately (abortive shutdown)
   */
  public void release() {
  }

  /**
   * Write to the socket up to len bytes from the buffer buf starting at
   * position pos.
   *
   * @param buf byte[] the buffer to write from
   * @param pos int starting position in buffer
   * @param len int number of bytes to write
   * @return int on success, the number of bytes written, which may be smaller
   *             than len; on failure, -1
   */
  public int write(byte[] buf, int pos, int len) {
    aa(this, sockType == SocketType.SENDER, "nonsender can't write");

    // fill buffer
    ppp("outbb size: " + outbb.capacity());
    ppp("outbb limit: " + outbb.limit());
    ppp("outbb remaining: " + outbb.remaining());
    return 0;
    int bytesCopied = Math.min(outbb.remaining(), len);
    outbb.put(buf, pos, bytesCopied);
    outbb.flip();

    ppp("bytes copied: " + bytesCopied);

    // read from buffer (to create packet) ONLY if we will actually send one
    int payloadLength = Math.min(Transport.MAX_PAYLOAD_SIZE, outbb.limit());
    if (this.seqNum + payloadLength <= sendBase + sendWindow) {
      byte[] payload = new byte[payloadLength];
      outbb.get(payload);
      outbb.compact();

      Transport t = makeTransport(Transport.DATA, seqNum, payload);
      tcpMan.sendData(this.tsid, t);

      p(this, "Write: converting to packet: " + TCPManager.bytesToString(payload));

      // only increment the seqnum if a packet is sent
      seqNum += payloadLength;
      // TODO(syu):
      // addTimer(1000, "sendData", new String[]{"TCPSockID", "int", "int", "[B"}, new Object[]{this.tsid, Transport.DATA, seqNum, payload});

    }

    // outgoingPacketStatus.put(seqNum, false);

    return bytesCopied;

  }

  /**
   * Read from the socket up to len bytes into the buffer buf starting at
   * position pos.
   *
   * @param buf byte[] the buffer
   * @param pos int starting position in buffer
   * @param len int number of bytes to read
   * @return int on success, the number of bytes read, which may be smaller
   *             than len; on failure, -1
   */
  public int read(byte[] buf, int pos, int len) {
    return -1;
  }

  public void addRecievingSock(TCPSock recvSock) {
    aa(this, this.sockType == SocketType.WELCOME, "non-welcome socket adding to queue");
    welcomeQueue.add(recvSock);
  }
  /*
   * Precondition: current state is 
   */
  public void synAckReceived() {
    aa(this, isConnectionPending(), "synAckReceived by invalid state");
    this.state = State.ESTABLISHED;
    p(this, "synAckReceived");
  }

  public void setTSID(TCPSockID tsid) {
    this.tsid = tsid;
  }
  public TCPSockID getTSID() {
    return this.tsid;
  }
  public int getLocalPort() {
    return this.tsid.getLocalPort();
  }
  public int getRemotePort() {
    return this.tsid.getRemotePort();
  }
  public int getLocalAddr() {
    return this.tsid.getLocalAddr();
  }
  public int getRemoteAddr() {
    return this.tsid.getRemoteAddr();
  }

  public SocketType getSockType() {
    return this.sockType;
  }

  public int getSendBase() {
    return sendBase;
  }

  public int getTransportSeqNum() {
    return this.seqNum++;
  }

  public TCPManager getTCPManager() {
    return this.tcpMan;
  }
  /*
   * End of socket API
   */

  /** 
   * Creates a transport packet with the specified parameters.
   *
   * @param type 
   * @param seqNum sequnce number. Note that this is NOT the current seqnum of this socket!
   * @param payload
   */
  public Transport makeTransport(int type, int seqNum, byte[] payload) {
    Transport t = new Transport(this.tsid.getLocalPort(), this.tsid.getRemotePort(),
        type, this.sendWindow, seqNum, payload);
    return t;
  }
}
