package syu;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

import static syu.Utils.*;

public class AcceptHandler implements IAcceptHandler, Debuggable {
  Dispatcher dispatcher;
  String id;

  @Override
  public String id() {
    return id;
  }

  public AcceptHandler (Dispatcher d) {
    this("AccepterHandler", d);
  }

  public AcceptHandler (String id, Dispatcher d) {
    this.id = id;
    this.dispatcher = d;
  }

  @Override
  public void onAccept(SelectionKey k) throws IOException {

    // SocketChannel clientChannel = dispatcher.getServerChannel().accept();
    SocketChannel clientChannel = ((ServerSocketChannel) k.channel()).accept();
    if (clientChannel == null)  {
      p(this, 4, "Client channel was null!");
        return;
    }

    p(this, 4, "accepted: " + port(clientChannel));
    clientChannel.configureBlocking(false);
    SelectionKey sk = clientChannel.register(dispatcher.selector, SelectionKey.OP_READ);
    NonblockingConnection nbc = new NonblockingConnection(clientChannel, sk, dispatcher);
    nbc.setHandler(new AsyncRequestHandler(nbc)); // questionable... circular refernces?
    sk.attach(nbc);
    //TODO(syu): REgister the new handler

    // this.dispatcher.createReadWriteHandler() 
    // {
  }
}
