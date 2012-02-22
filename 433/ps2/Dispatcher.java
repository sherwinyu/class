package syu;

import java.nio.channels.*;
import java.io.IOException;
import java.util.*; 

import static syu.Utils.*;

public class Dispatcher implements Runnable, Debuggable {

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
    this.accepterKey = this.register(this.serverChannel, new Accepter(this), SelectionKey.OP_ACCEPT);
    alive = true;
  }

  public SelectionKey register(SelectableChannel selectableChannel, IHandler h, int ops) {
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

  protected void eventLoop() throws IOException {
    while(alive) { //TODO(syu) is this necessary?
      Set<SelectionKey> keys = getNewEvents();
      for (SelectionKey key : keys) {
        if(key.isAcceptable()) {
          handleAccept(key);
        }
        if(key.isWritable()) {
          handleWrite(key);
        }
        if(key.isReadable()) {
          handleRead(key);
        }
      }
    }
  }

  protected void handleAccept(SelectionKey key) throws IOException {
    ((IHandler) key.attachment()).onAccept(key);
   }
  protected void handleRead(SelectionKey key) throws IOException {
    ((IHandler) key.attachment()).onRead(key);
  }
  protected void handleWrite(SelectionKey key) throws IOException {
    ((IHandler) key.attachment()).onWrite(key);
  }

}
