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
  public void dumpState(int t) {
    switch (sockType) {
      case SENDER:
        p(this, t, "seqNum: " + seqNum);
        p(this, t, "sendBase: " + sendBase);
        p(this, t, "sendWindow: " + sendWindow);
        p(this, t, "sendbb position: " + sendbb.position());
        p(this, t, "sendbb limit: " + sendbb.limit());
        p(this, t, "sendbb remaining: " + sendbb.remaining());
        break;
      case RECEIVER:
        p(this, t, "recvBase: " + recvBase);
        p(this, t, "recvbb position: " + recvbb.position());
        p(this, t, "recvbb limit: " + recvbb.limit());
        p(this, t, "recvbb remaining: " + recvbb.remaining());
        break;
      case WELCOME:
        break;
    }
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

  public boolean isSender() {
    return sockType == SocketType.SENDER;
  }
  public boolean isReceiver() {
    return sockType == SocketType.RECEIVER;
  }
  public boolean isWelcome() {
    return sockType == SocketType.WELCOME;
  }

  public static final int SEND_BUFFER_SIZE = 1000;
  public static final int RECV_BUFFER_SIZE = 1000;

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
  ByteBuffer sendbb;

  // Receive sockets
  private int recvBase;
  HashMap<Integer, Transport> transportBuffer;
  ByteBuffer recvbb;

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
    sendWindow = 3 * Transport.MAX_PAYLOAD_SIZE;
    sendBase = 0;
    seqNum = 0;
    sendbb = ByteBuffer.allocate(SEND_BUFFER_SIZE);
  }

  private void initReceiver() {
    aa(this, sockType == SocketType.RECEIVER, "");
    recvBase = 0;
    recvbb = ByteBuffer.allocate(RECV_BUFFER_SIZE);
    transportBuffer = new HashMap<Integer, Transport>();
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
    // if (tcpMan.isPortFree(localPort)) {
    this.tsid.localPort = localPort;
    return 0;
    // }
    // return -1;
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
    this.state = State.LISTEN;
    p(this, 3, "listening. backlog: " + backlog);

    this.backlog = backlog;
    welcomeQueue = new ArrayList<TCPSock>(backlog);
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

    p(this, 3, "accept()");
    TCPSock recvSock = welcomeQueue.remove(0);
    recvSock.state = State.ESTABLISHED;
    p(recvSock, 4, "established");
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
    if (!isClosed() || !isSender()) {
      pe(this, " connect() called on inappropriated");
      return -1;
    }

    p(this, 3, "Connect() to:  " + destAddr + ":" + destPort);
    this.tsid.remoteAddr = destAddr;
    this.tsid.remotePort = destPort;

    tcpMan.add(this.tsid, this);
    tcpMan.sendSYN(this.tsid);
    this.state = State.SYN_SENT;

    return 0;
  }

  /**
   * Initiate closure of a connection (graceful shutdown)
   */
  public void close() {
    p(this, 3, "Close()");
    switch (sockType) {
      case SENDER:
        state = State.SHUTDOWN;
        if (sendbb.position() == 0)
          release();
        break;
      case RECEIVER:
        release();
        break;
      case WELCOME:
        release();
    }

  }

  /**
   * Release a connection immediately (abortive shutdown)
   */
  public void release() {
    p(this, 3, "Release()");
    // TODO(syu) send a FIN
    this.state = State.CLOSED;
    tcpMan.sendFIN(tsid);
    if (isReceiver())
      tcpMan.remove(tsid);
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
    aa(this, isSender(), "nonsender can't write");
    aa(this, isConnected(), "nonsender can't write");

    // fill buffer
    p(this, 3, "write called"); //\n\t buf = " + TCPManager.bytesToString(buf)); p(this, 4, "sendbb position: " + sendbb.position());
    dumpState(4);
    // p(this, 4, "sendbb limit: " + sendbb.limit());
    // p(this, 4, "sendbb remaining: " + sendbb.remaining());
    // p(this, 4, "sendbb size: " + sendbb.capacity());

    int bytesCopied = Math.min(sendbb.remaining(), len);
    sendbb.put(buf, pos, bytesCopied);
    p(this, 3, "bytes copied: " + bytesCopied);

    // try to send as much as possible
    sendFromBuffer();
    return bytesCopied;

  }

  /**
   * sends data from buffer.
   *
   * precondition: sendbb is in put mode
   * post condition: sendbb is in put mode
   */
  private void sendFromBuffer() {
    aa(this, isSender(), "nonsender can't write");
    aa(this, isConnected() || isClosurePending()  , "needs to be established can't write");

    // switch sendbb to get mode
    sendbb.flip();
    p(this, 3, "Sending from buffer. bb flipped.");
    int payloadLength = Math.min(Transport.MAX_PAYLOAD_SIZE, sendbb.limit());
    p(this, 4, "Max Payload size: " +  Transport.MAX_PAYLOAD_SIZE);
    p(this, 4, "payloadlen: " + payloadLength);
    dumpState(4);


    if (roomForPacket(payloadLength) && payloadLength > 0) {
      byte[] payload = new byte[payloadLength];
      sendbb.get(payload);

      Transport t = makeTransport(Transport.DATA, seqNum, payload);
      tcpMan.sendData(this.tsid, t);

      p(this, 3, "Write: converting to packet: " + TCPManager.bytesToString(payload));

      // only increment the seqNum if a packet is sent
      seqNum += payloadLength;
    }

    // switch back to put mode
    sendbb.compact();
    dumpState(4);
    if (isClosurePending() && sendbb.position() == 0)
      release();
      
  }

  // Returns true if there is enough room in the window for a packet with payloadLength payload
  private boolean roomForPacket(int payloadLength) {
    return this.seqNum + payloadLength <= this.sendBase + this.sendWindow;
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
    aa(this, isReceiver(), "nonreceiver socket reading");
    aa(this, state == State.ESTABLISHED, "attempting to read from closed socket");

    recvbb.flip();
    int bytesCopied = Math.min(recvbb.limit(), len);
    recvbb.get(buf, pos, bytesCopied);
    recvbb.compact();

    return bytesCopied;
  }

  public void writeFromBuffer() {
    aa(this, isSender(), "non-sender socket writing");
    aa(this, state == State.ESTABLISHED || state == State.SHUTDOWN, "attempting to write from non established socket");
  }

  public void addRecievingSock(TCPSock recvSock) {
    aa(this, isWelcome(), "non-welcome socket adding to queue");
    welcomeQueue.add(recvSock);
  }

  public void synAckReceived() {
    aa(this, isConnectionPending(), "synAckReceived by invalid state");
    this.state = State.ESTABLISHED;
    p(this, "synAckReceived");
  }

  public void dataReceived(Transport t) {
    aa(this, isReceiver(), "DATA received by non receiver socket");
    aa(this, isConnected(),  "DATA received by invalid socket state");

    int inSeqNum = t.getSeqNum();

    p(this, 3, "dataReceived() seqnum: " + inSeqNum);
    transportBuffer.put(inSeqNum, t);
    deliverToBuffer();

    dumpState(4);
    p(this, 3, "ending dataReceived");

    tcpMan.sendACK(this.tsid);
  }

  private void deliverToBuffer () {
    while (transportBuffer.containsKey(recvBase)) {
      p(this, 4, "coalescing at recvBase: " + recvBase);
      dumpState(6);

      byte[] payload = transportBuffer.get(recvBase).getPayload();

      if (recvbb.remaining() >= payload.length) {
        recvbb.put(payload);
        recvBase += payload.length;
        p(this, 5, "coalesced: recvBase incremented to " + recvBase);
      }
      else {
        p(this, 5, "coalesce ending: buffer full");
        break;
      }
    }
  }


  public void dataAckReceived(Transport t) {
    aa(this, isClosurePending() || isConnected(), "dataAckReceived by invalid state");
    aa(this, isSender(), "dataAckReceived by non sender");
    if (t.getSeqNum() > this.sendBase) {
      this.sendBase = t.getSeqNum();
      p(this, 3, "dataAckReceived: advancing sendBase to " + sendBase);
      sendFromBuffer();
    }
    else 
      p(this, 3, "dataAckReceived: ignored sendBase >= ackNum: " + sendBase + " > " + t.getSeqNum());
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

  public SocketType getSockType2() {
    return this.sockType;
  }

  public int getSendBase() {
    return sendBase;
  }

  public int getTransportSeqNum() {
    return this.seqNum;
  }
  public int getRecvBase() {
    return this.recvBase;
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
