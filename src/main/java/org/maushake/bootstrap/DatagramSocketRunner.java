/*
 * Copyright (c) 2014 to present, doctainer gmbh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.maushake.bootstrap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * reads from a socket and places received datagrams in a queue.
 *  after some time of idleness while waiting for input: fetches datagrams from another queue and sends them over the socket
 * @author jm
 */
public class DatagramSocketRunner implements DatagramOutbound, Runnable {
    private final Logger logger;
    private boolean shouldRun = true;
	static final int DATAGRAM_LENGTH = 1024;
	final DatagramInbound receivedFromSocket;
	GenericQueue<DatagramPacket> toBeSentToSocket;
	final DatagramSocket socket;
	
	public DatagramSocketRunner(DatagramInbound receivedFromSocket, DatagramSocket socket) {
		this.logger = LoggerFactory.getLogger(this.getClass());
		this.receivedFromSocket = receivedFromSocket; 
		this.toBeSentToSocket = new GenericQueue<DatagramPacket>();
		this.socket = socket;
	}
	public void run() {
		if(logger.isTraceEnabled()) {
			logger.trace("run()");
		}	
	
		// System.out.println(this.getClass().getSimpleName()+".run()");
		while (shouldRun) {
			// listen for a reply packet *
			
			try {
				// o5140602 optimize SocketRunnable
				//  TODO allocate buffer outside of loop
				byte[] buf = new byte[DATAGRAM_LENGTH];
				DatagramPacket receivedPacket = new DatagramPacket(buf, DATAGRAM_LENGTH);
				socket.receive(receivedPacket); // timeout should be in effect
				// o5140602 optimize SocketRunnable
				//  put pure bytes into Q ?
				// TODO trace statement
				// System.out.println("!!! Datagram received !!!");
				byte[] data = receivedPacket.getData(); 
				int l = receivedPacket.getLength();
				byte[] rxBytes = new byte[l];
				System.arraycopy(data, 0, rxBytes, 0, l);
				// o5140602 decons handle_info
				receivedFromSocket.processDg(receivedPacket.getAddress(), receivedPacket.getPort(), rxBytes);
			}
			catch (SocketTimeoutException ste) {
				/* ignored; this exception is by design to
				 * break the blocking from socket.receive */
			}
			catch (IOException ioe) {
				System.err.println("Unexpected exception: "+ioe);
				ioe.printStackTrace();
				/* resume operation */
			}
			DatagramPacket obj = toBeSentToSocket.tryGet(); // non-blocking
			if(obj != null) {
				try {
					socket.send(obj);
				} catch(IOException ioex) {
					
				}
			}
		}
	}
	
	/**
	 * implements {@link DatagramOutbound}
	 */
	@Override
	public void deliverDg(DatagramPacket dg) {
		// trace
		System.out.println(this.getClass().getSimpleName()+".deliver(...)");
		toBeSentToSocket.put(dg);
	}
}
