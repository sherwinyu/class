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
    return tcpMan.node.id() + "_TCPSock" + sockType +" " + localPort;
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


  private State state;
  private SocketType sockType;

  private TCPManager tcpMan;

  private int backlog;
  private int localPort;
  int seqNum;

  public TCPSock(TCPManager tcpMan, SocketType type) {
    this.tcpMan = tcpMan;
    this.state = State.CLOSED;
    this.sockType = type;
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
      this.localPort = localPort;
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
      pe(this, "not a welcome socket");
      return -1;
    }

    this.backlog = backlog;
    this.state = State.LISTEN;
    tcpMan.addWelcomeSocket(this);
    return 0;

  }

  /**
   * Accept a connection on a socket
   *
   * @return TCPSock The first established connection on the request queue
   */
  public TCPSock accept() {
    return null;
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

    // TODO(syu): how to choose window size and ttl?
    int window = 555;
    int ttl = 555;
    Transport t = new Transport(this.localPort, destPort, Transport.SYN, window, this.seqNum, new byte[]{});
    Packet p = new Packet(destAddr, tcpMan.getAddr(), ttl, Protocol.TRANSPORT_PKT, tcpMan.getPacketSeqNum(), t.pack());
    tcpMan.node.send(destAddr, p);

    this.state = State.SYN_SENT;
    tcpMan.add(TCPSockID.fromPacket(p), this);

    return 0;

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
    return -1;
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

  public int getLocalPort() {
    return this.localPort;
  }

  public SocketType getSockType() {
    return this.sockType;
  }

  public TCPManager getTCPManager() {
    return this.tcpMan;
  }
  /*
   * End of socket API
   */
}
