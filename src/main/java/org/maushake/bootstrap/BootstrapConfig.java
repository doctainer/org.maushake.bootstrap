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

/**
 * Represents configuration
 *   
 * @author jm
 * 
 */
public class BootstrapConfig {
	
public String connect_regex() {
	return ConfigDefaults.CONNECT_REGEX;
}
public int primary_port() {
	return ConfigDefaults.PRIMARY_PORT;
}
public int[] secondary_posts() {
	return ConfigDefaults.SECONDARY_PORTS;
}
public String multicast_ip() {
	return ConfigDefaults.MULTICAST_IP;
}
public int multicast_ttl() {
	return ConfigDefaults.MULTICAST_TTL;
}
public Transport getTransport() {
	return TransportEnum.Multicast;
}


public int ping_timeout() {
	return ConfigDefaults.PING_TIMEOUT;
}

//use -1 to represent "infinity"
 public int min_connections() {
	 return ConfigDefaults.CONNECTIONS;
 }
}
