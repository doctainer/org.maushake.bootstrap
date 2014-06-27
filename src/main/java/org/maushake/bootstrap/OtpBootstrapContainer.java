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

import java.util.HashMap;
import java.util.Map;

import com.ericsson.otp.erlang.OtpNode;

/**
 * contains all running instances of the bootstrap protocol
 * @author jm
 * <pre>
 *  o5140624 bind bootstap protocol
 * </pre>
 */
public class OtpBootstrapContainer {
 private static class Elem {
	 final ConnectionController connectionCtl;
	 final BootstrapProtocol bootstrapProtocol;
	 final Thread theThread;
	 public Elem(ConnectionController connectionCtl, BootstrapProtocol bootstrapProtocol, Thread theThread) {
		 this.connectionCtl = connectionCtl;
		 this.bootstrapProtocol = bootstrapProtocol;
		 this.theThread = theThread;
	 }
 }
 
 private final BootstrapConfig bootstrapConfig;
 private final Map<String, Elem> protocolInstances;
 
 public OtpBootstrapContainer(BootstrapConfig bootstrapConfig) {
	 this.bootstrapConfig = bootstrapConfig;
	 protocolInstances = new HashMap<String, OtpBootstrapContainer.Elem>();
 }
 
  
 public synchronized void addNode(OtpNode otpNode) {
	 String nodeName = otpNode.node();
	 OtpConnectionController connectionCtl = OtpConnectionController.create(otpNode, bootstrapConfig);
	 BootstrapProtocol bootstrapProtocol = new BootstrapProtocol(nodeName, connectionCtl, bootstrapConfig);
	 Thread t = new Thread(bootstrapProtocol);
     protocolInstances.put(nodeName, new Elem(connectionCtl, bootstrapProtocol, t));
     t.start();
 }
 
 public synchronized void addNode(ConnectionController conCtrl, OtpNode otpNode) {
	 String nodeName = otpNode.node();
	 BootstrapProtocol bootstrapProtocol = new BootstrapProtocol(nodeName, conCtrl, bootstrapConfig);
	 Thread t = new Thread(bootstrapProtocol);
     protocolInstances.put(nodeName, new Elem(conCtrl, bootstrapProtocol, t));
     t.start();
 }
}
