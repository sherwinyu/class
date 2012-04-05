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

  public TCPManager(Node node, int addr, Manager manager) {
    this.node = node;
    this.addr = addr;
    this.packetSeqNum = 0;

    this.manager = manager;
    this.sockSpace = new HashMap<TCPSockID, TCPSock>();
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
    add(new TCPSockID(addr, -1, sock.getLocalPort(), -1), sock);
  }

  /*
   * End Socket API
   */

  public int getAddr() {
    return this.node.getAddr();
  }

  //TODO(syu): implement this
  public boolean isPortFree(int localPort) {
    return true;
  }

  public int getPacketSeqNum() {
    return packetSeqNum++;
  }

}
