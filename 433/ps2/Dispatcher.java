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
  protected AsyncServer server;
  private ServerSocketChannel serverChannel;

  protected ServerSocketChannel getServerChannel() {
    return serverChannel;
  }

  public Dispatcher(AsyncServer server, ServerSocketChannel serverChannel) throws IOException {
    this.server = server;
    this.serverChannel = serverChannel;
    this.selector = Selector.open();
    this.register(this.serverChannel, new AcceptHandler(this, this.serverChannel), SelectionKey.OP_ACCEPT);
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
      while(true) { //TODO(syu) is this necessary?
        Set<SelectionKey> keys = getNewEvents();

        p(this, "New Iteration. keys = " + keys);
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {

          SelectionKey key = it.next();

          if(key.isAcceptable()) {
            p(this, 3, "key acceptable: " + ((ServerSocketChannel) key.channel()) );
            handleAccept(key);
          }
          if(key.isReadable()) {
            p(this, 3, "key readable: " + port((SocketChannel) key.channel()) );
            handleRead(key);
          }
          if(key.isWritable()) {
            handleWrite(key);
          }
          it.remove();
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
    ((IReadHandler) ((NonblockingConnection) key.attachment()).handler).onRead(key);

  }
  protected void handleWrite(SelectionKey key) throws IOException {
    ((IWriteHandler) ((NonblockingConnection) key.attachment()).handler).onWrite(key);
  }

}
