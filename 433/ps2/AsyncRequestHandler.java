package syu;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

import static syu.Utils.*;

public class AsyncRequestHandler extends RequestHandler implements IReadHandler, IWriteHandler  {

  private NonblockingConnection nc;
  private SocketChannel clientChannel;
  ByteBuffer inbuffer;


  public void setState(ConnectionState state) {
    getConnection().state = state;
  }
  
  public void unregisterOp(int op) {
    //TODO
        //getConnection().unregister(SelectionKey.OP_READ);
  }

  public NonblockingConnection getConnection() {
    return this.nc;
  }

  public AsyncRequestHandler(NonblockingConnection nc) {
    this.nc = nc;
    this.clientChannel = getConnection().clientChannel;
    this.id = "ARHport#" + port(this.clientChannel);
    this.inbuffer = getConnection().inbuffer;
    p(this, 4, "created");
  }
  /* 
   * Precondition: this.connection.state = ACCEPTED or READING
   * Set state to READING
   * onRead should read data from the client into the buffer
   * if getClient().read(buffer) returns -1
   *    unregister interest in 
   *    then we are ready to begin processing the request
   *    set State to PROCESSING
   *    call Process -- this can either be delegate to a thread, or not
   * else, return 
   * 
   */
  @Override
    public void onRead() throws IOException {
      setState(ConnectionState.READING);

      if (clientChannel.read(inbuffer) == -1) {
        unregisterOp(SelectionKey.OP_READ);
        setState(ConnectionState.PROCESSING);
        processRequest();
      }
    }

  /*
   * Precondition: getConnection.state == PROCESSING
   * interestOps = nothing
   * Called when the data has been read
   * Can either be done synchronously (right after onRead, in same thread) or async with a thread
   * When done, should set interestOps to Write
   */

  public void processRequest() {
    //TODO(syu)
  }

  @Override
    public void onWrite() throws IOException {

    }

}
