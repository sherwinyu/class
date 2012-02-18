/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*;
import java.net.*;
import java.util.*;

class SimpleWebServer{

    public static int serverPort = 6789;    
    //public static String WWW_ROOT = "/home/httpd/html/zoo/classes/cs433/";
    public static String WWW_ROOT = "./";

    public static void main(String args[]) throws Exception  {
	
	// see if we do not use default server port
	if (args.length >= 1)
	    serverPort = Integer.parseInt(args[0]);

	// see if we want a different root
	if (args.length >= 2)
	    WWW_ROOT = args[1];

	// create server socket
	ServerSocket listenSocket = new ServerSocket(serverPort);
	System.out.println("server listening at: " + listenSocket);
	System.out.println("server www root: " + WWW_ROOT);

	while (true) {

	    try {

		// take a ready connection from the accepted queue
		Socket connectionSocket = listenSocket.accept();
		System.out.println("receive request from " + connectionSocket);
	
		// process a request
		WebRequestHandler wrh = 
		    new WebRequestHandler( connectionSocket, WWW_ROOT );

		Thread t = new Thread(wrh);
		t.start();

	    } catch (Exception e)
		{
		}
	} // end of while (true)
	
    } // end of main

} // end of class SimpleWebServer
