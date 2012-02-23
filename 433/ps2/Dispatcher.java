package syu;

import java.nio.channels.*;
import java.io.IOException;
import java.util.*; 

import static syu.Utils.*;

interface IDispatcher extends Runnable {
  public void eventLoop() throws IOException;
  public SelectionKey register(SelectableChannel c, IAsyncHandler h, int ops) throws IOException;
}

public class Dispatcher implements IDispatcher, Debuggable {

  String id = "Dispatcher";
  public String id() {
    return id;
  }
  Selector selector;
  boolean alive;
  SelectionKey accepterKey;

  private ServerSocketChannel serverChannel;
  
  protected ServerSocketChannel getServerChannel() {
    return serverChannel;
  }

  public Dispatcher(ServerSocketChannel serverChannel) throws IOException {
    this.serverChannel = serverChannel;
    this.selector = Selector.open();
    this.accepterKey = this.register(this.serverChannel, new AcceptHandler(this), SelectionKey.OP_ACCEPT);
    alive = true;
  }

  public SelectionKey register(SelectableChannel selectableChannel, IAsyncHandler h, int ops) {
    SelectionKey key = null ;
    try {
      key = selectableChannel.register(selector, ops);
      key.attach(h);
    }
    catch (ClosedChannelException e) {
      p(this, "Key not registered: channel closed.");
      e.printStackTrace();
      System.exit(0);
    }
    return key;
  }

  @Override
  public void run () {
    try {
      eventLoop();
    }
    catch (IOException e) {
      p("Dispatcher: IOException: " + e.getMessage());
      e.printStackTrace();
    }
  }

  protected Set<SelectionKey> getNewEvents() throws IOException {
    selector.select();
    return selector.selectedKeys();
  }

  @Override
  public void eventLoop() throws IOException {
    while(alive) { //TODO(syu) is this necessary?
      Set<SelectionKey> keys = getNewEvents();
      p(this, "New Iteration. keys = " + keys);
      for (SelectionKey key : keys) {
        if(key.isAcceptable()) {
          p(this, 3, "key: " + ((ServerSocketChannel) key.channel()) + " is acceptable");
          handleAccept(key);
        }
        if(key.isReadable()) {
          p(this, 3, "key: " + port((SocketChannel) key.channel()) + " is readable");
          handleRead(key);
        }
        if(key.isWritable()) {
          handleWrite(key);
        }
      }
    }
  }

  public  NonblockingConnection getConnectionFromKey(SelectionKey key) {
    return ((NonblockingConnection) key.attachment());
  }


  protected void handleAccept(SelectionKey key) throws IOException {
    // getConnectionFromKey(key).getHandler().
    ((IAcceptHandler) key.attachment()).onAccept(key);
   }
  protected void handleRead(SelectionKey key) throws IOException {
    ((IReadHandler) ((NonblockingConnection) key.attachment()).handler).onRead();
  }
  protected void handleWrite(SelectionKey key) throws IOException {
    ((IWriteHandler) key.attachment()).onWrite();
  }

}
