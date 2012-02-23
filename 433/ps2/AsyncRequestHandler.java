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
  ByteBuffer outbuffer;


  public void setState(ConnectionState state) {
    getConnection().state = state;
  }
  
  public void setOps(int ops) {

    getConnection().key.interestOps(ops); //unregister(SelectionKey.OP_READ);
  }

  public NonblockingConnection getConnection() {
    return this.nc;
  }

  public AsyncRequestHandler(NonblockingConnection nc) {
    this.nc = nc;
    this.clientChannel = getConnection().clientChannel;
    this.parentServer = getConnection().dispatcher.server;
    this.id = "ARHport#" + port(this.clientChannel);
    this.inbuffer = getConnection().inbuffer;
    this.outbuffer = getConnection().outbuffer;
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
String readSoFar = "";
  @Override
    public void onRead(SelectionKey sk) throws IOException {
      setState(ConnectionState.READING);
      p(this, 4, "reading");
    clientChannel.read(inbuffer);
    readSoFar = new String(inbuffer.array(), 0, inbuffer.position() + 1);
    p(this, 4, "read so far:" + readSoFar);
      if (readSoFar.indexOf("\r\n\r\n") != -1) {
        setOps(getConnection().key.interestOps() & ~ SelectionKey.OP_READ);
        // unregisterOp(SelectionKey.OP_READ);
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
    // WebRequest req.fromString(

    p(this, 4, "processing request");
    String responseString = getResponseString(readSoFar);
    p(this, 4, "response = " + responseString);
    setState(ConnectionState.WRITING);
    setOps(getConnection().key.interestOps() | SelectionKey.OP_WRITE);
    outbuffer.put(responseString.getBytes());
    outbuffer.flip();
  }

  @Override
    public void onWrite(SelectionKey k) throws IOException {
      clientChannel.write(outbuffer);
      if (!outbuffer.hasRemaining()) { 
        setState(ConnectionState.WRITTEN);
        setOps(0);
        getConnection().cleanup();
      }
    }

}
