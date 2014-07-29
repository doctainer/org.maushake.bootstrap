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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum TransportEnum implements Transport {
 Broadcast() {

	@Override
	public DatagramSocket openSocket(BootstrapConfig theConfig, int port) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetAddress[] addresses(BootstrapConfig theConfig) {
		// TODO Auto-generated method stub
		return null;
	}
	 
 },
 Multicast() {
    private final Logger logger = LoggerFactory.getLogger("MultiCast");
	@Override
	public DatagramSocket openSocket(BootstrapConfig theConfig, int port) throws IOException {
		if(logger.isTraceEnabled()) {
			logger.trace("openSocket()");
		}
		// System.out.println(this.toString()+".openSocket()");
		MulticastSocket socket;
		socket = new MulticastSocket(null); // see http://stackoverflow.com/questions/13558522/binding-to-a-specific-ip-address-and-port-to-receive-udp-data#
		socket.setReuseAddress(false);
		SocketAddress socketAddress = new InetSocketAddress(port);
		try {
		socket.bind(socketAddress);
		} catch(IOException ex) {
			if(logger.isTraceEnabled()) {
				logger.trace("openSocket("+port+") !!! "+ex.toString());
			}	
		  // System.out.println(this.toString()+".openSocket("+port+") !!! "+ex.toString());	
		  throw ex;
		}
		InetAddress multicastAddressGroup = InetAddress.getByName(theConfig.multicast_ip());
		socket.joinGroup(multicastAddressGroup);
		// 
		socket.setSoTimeout(250);
		return socket;
	}

	@Override
	public InetAddress[] addresses(BootstrapConfig theConfig) throws IOException {
		InetAddress multicastAddressGroup = InetAddress.getByName(theConfig.multicast_ip());
		return new InetAddress[]{multicastAddressGroup};
	}
	 
 }
}
