package syu;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

import static syu.Utils.*;

public interface IHandler extends Debuggable {
  public void onAccept(SelectionKey key) throws IOException;
  public void onRead(SelectionKey key) throws IOException;
  public void onWrite(SelectionKey key) throws IOException;
}

abstract class Handler implements IHandler {
  String id;
  public String id() {
    return id;
  }
}

class Accepter extends Handler {
  Dispatcher dispatcher;

  public Accepter (Dispatcher d) {
    this("AccepterHandler", d);
  }
  public Accepter (String id, Dispatcher d) {
    this.id = id;
    this.dispatcher = d;
  }

  public void onAccept(SelectionKey key) throws IOException {
    SocketChannel clientChannel = dispatcher.getServerChannel().accept();
    clientChannel.configureBlocking(false);
    p(this, "accepted: " + clientChannel);
    // this.dispatcher.createReadWriteHandler() 
    // {
  }

  public void onRead(SelectionKey key) {

  }

  public void onWrite(SelectionKey key) {

  }

}
/*
	ISocketReadWriteHandlerFactory echoFactory =
	    new EchoLineReadWriteHandlerFactory();
	Acceptor acceptor = new Acceptor( sch, dispatcher, echoFactory );
        */
