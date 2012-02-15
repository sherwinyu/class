import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

public class EchoServer {

    private static Selector selector;

    public static int DEFAULT_PORT = 6789;
  
    private static boolean DEBUG = true;

    private static void DEBUG(String s) {
	if (DEBUG) {
	    System.out.println(s);
	}
    }

    private static ServerSocketChannel openServerSocketChannel(int port) {
	ServerSocketChannel serverChannel = null;

	try {
	    // create server channel
	    serverChannel = ServerSocketChannel.open();
	    // extract server socket of the server channel and bind the port
	    ServerSocket ss = serverChannel.socket();
	    InetSocketAddress address = new InetSocketAddress(port);
	    ss.bind(address);

	    // configure it to be non blocking
	    serverChannel.configureBlocking(false);
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);   
	}

	return serverChannel;
		
    } // end of openServerSocketChannel

    private static void handleAccept(SelectionKey key) throws IOException {
	ServerSocketChannel server = (ServerSocketChannel) key.channel();

	// extract the ready connection
	SocketChannel client = server.accept();
	DEBUG("handleAccept: Accepted connection from " + client);

	// configure the connection to be non-blocking
	client.configureBlocking(false);

	// register the new connection with read and write events/operations
	SelectionKey clientKey = 
	    client.register(
			    selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		
	// attach a buffer to the new connection
	ByteBuffer buffer = ByteBuffer.allocate(100);
	clientKey.attach(buffer);
    } // end of handleAccept

    private static void handleRead(SelectionKey key) throws IOException {
	// a connection is ready to be read
	SocketChannel client = (SocketChannel) key.channel();
	ByteBuffer output = (ByteBuffer) key.attachment();
	int readBytes = client.read(output);
	DEBUG("handleRead: Read data from connection " + client 
	      + " for " + readBytes + " byte(s); to buffer " + output);
	try {Thread.sleep(5000);} catch (InterruptedException e) {}

    } // end of handleRead

    private static void handleWrite(SelectionKey key) throws IOException {
	SocketChannel client = (SocketChannel) key.channel();
	ByteBuffer output = (ByteBuffer) key.attachment();
	output.flip();
	int writeBytes = client.write(output);
	output.compact();
	DEBUG("handleWrite: Write data to connection " + client  
	      + " for " + writeBytes 
	      + " byte(s); from buffer " + output);
	try {Thread.sleep(5000);} catch (InterruptedException e) {}
    } // end of handleWrite

    public static void main(String[] args) {
  
	int port;
	try {
	    port = Integer.parseInt(args[0]);
	}
	catch (Exception ex) {
	    port = DEFAULT_PORT;   
	}
	DEBUG("Listening for connections on port " + port);

		
	try {

	    // create selector
	    selector = Selector.open();

	    // open server socket for accept
	    ServerSocketChannel serverChannel = openServerSocketChannel(port);

	    // register the server channel to a selector	
	    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);   
	}

    
	while (true) {
      
	    DEBUG("Enter selection");
	    try {
		// check to see if any events
		selector.select();
	    }
	    catch (IOException ex) {
		ex.printStackTrace();
		break;
	    }
        
	    // readKeys is a set of ready events
	    Set readyKeys = selector.selectedKeys();

	    // create an iterator for the set
	    Iterator iterator = readyKeys.iterator();

	    // iterate over all events
	    while (iterator.hasNext()) {
        
		SelectionKey key = (SelectionKey) iterator.next();
		iterator.remove();

		try {
		    if (key.isAcceptable()) { // a new connection is ready to be accepted
			handleAccept(key);
		    } // end of isAcceptable

		    if (key.isReadable()) {
			handleRead(key);
		    } // end of isReadable

		    if (key.isWritable()) {
			handleWrite(key);
		    } // end of if isWritable
		}
		catch (IOException ex) {
		    key.cancel();
		    try {
			key.channel().close();
		    }
		    catch (IOException cex) {}
		} // end of catch
				
	    } // end of while (iterator.hasNext()) {

	} // end of while (true)
  
    } // end of main

} // end of class
