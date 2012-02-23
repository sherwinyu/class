package syu;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

import static syu.Utils.*;

public interface IAsyncHandler {
}

interface IReadHandler extends IAsyncHandler {
  public void onRead() throws IOException;
}

interface IAcceptHandler extends IAsyncHandler {
  public void onAccept(SelectionKey k) throws IOException;
}

interface IWriteHandler extends IAsyncHandler {
  public void onWrite() throws IOException;
}

/*
	ISocketReadWriteHandlerFactory echoFactory =
	    new EchoLineReadWriteHandlerFactory();
	Acceptor acceptor = new Acceptor( sch, dispatcher, echoFactory );
        */
