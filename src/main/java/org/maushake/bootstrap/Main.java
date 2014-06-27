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

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
public static void main(String[] args) throws UnknownHostException {
	byte x = (byte)128;
	int k = x & 0xff;
	// System.out.println(k);
	
	// byte[] pingAddrBytes = new byte[]{(byte)239,(byte)192,0,1};
	// InetAddress inetAddress = InetAddress.getByAddress(pingAddrBytes);
	InetAddress inetAddress = InetAddress.getByName("2001:0db8:85a3:08d3:1319:8a2e:0370:7344");
	byte[] addr = inetAddress.getAddress();
	// Integer.toHexString(addr[0] & 0xff).
	int l = 8;
	for(int i=0; i<8; i++) {
		
	}
	System.out.println(Integer.toHexString(addr[0] & 0xff) + Integer.toHexString(addr[1] & 0xff));
	
	System.out.println( InetAddress.getLocalHost().getHostName());
			// getHostName();
}
}
