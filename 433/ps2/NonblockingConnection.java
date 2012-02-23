package syu;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

import static syu.Utils.*;

/*
 * Maintains state and context of an asynchronous request
 */

public class NonblockingConnection implements Debuggable {
  String id;
  SocketChannel clientChannel;
  ConnectionState state;
  SelectionKey key;
  IAsyncHandler handler;
  Dispatcher dispatcher;
  ByteBuffer inbuffer;
  ByteBuffer outbuffer;

  public String id() {
    return id;
  }

  public NonblockingConnection(SocketChannel clientChannel, SelectionKey sk,  Dispatcher d) {

    this.clientChannel = clientChannel;
    this.id = "Connection#" + port(this.clientChannel);
    this.key = sk;
    this.handler = null;
    this.dispatcher = d;
    this.inbuffer = ByteBuffer.allocate(4096);
    this.outbuffer = ByteBuffer.allocate(4096);
    this.state = ConnectionState.ACCEPTED;
    p(this, 4, "new connection created.");
  }

  public void setHandler(IAsyncHandler h) {
    this.handler = h;
  }
}

enum ConnectionState {NONE, ACCEPTED, READING, PROCESSING, WRITING, CLOSED };
