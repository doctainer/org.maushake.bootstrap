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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.otp.erlang.OtpNode;
import com.ericsson.otp.erlang.OtpNodeStatus;

/**
 * Provides a jinterface-based ConnectionController
 * 
 * @author jm
 * 
 */
public class OtpConnectionController implements OtpNodeUpDownListener,
		ConnectionController {

	private final OtpNode otpNode;
	private final String pattern;
	final int minimum;
	private final Logger logger;
	/**
	 * Although an OtpNode maintains a map called connections which maps each
	 * remote node name to its cooked connection, this data is encapsulated and
	 * not availabne to us. So we need to reproduce this information here
	 */
	Map<String, Date> connectedNodes;

	private OtpConnectionController(OtpNode otpNode, BootstrapConfig bootstrapConfig) {
		this.otpNode = otpNode;
		this.pattern = bootstrapConfig.connect_regex();
		this.minimum = bootstrapConfig.min_connections();
		this.connectedNodes = new ConcurrentHashMap<String, Date>();
		this.logger = LoggerFactory.getLogger(OtpConnectionController.class);
	}

	/**
	 * avoid registration during construction
	 */
	public static OtpConnectionController create(OtpNode otpNode, BootstrapConfig bootstrapConfig) {
		OtpConnectionController result = new OtpConnectionController(otpNode, bootstrapConfig);
		otpNode.registerStatusHandler(new OtpNodeStatusAdapter(result));
		return result;
	}

	@Override
	public void maybe_connect(String remoteNode) {

		if (match(remoteNode, pattern)) {
			otpNode.ping(remoteNode, 5000);
		}
	}

	protected boolean match(String nodeName, String pattern) {
		if(logger.isTraceEnabled()) {
			logger.trace("match("+nodeName+","+pattern+")");
		}
		// perform some regex stuff
		return nodeName.matches(pattern);
	}

	
	/**
	 * this method semantically matches erlang:nodes(connected)
	 */
	private List<String> nodesConnected() {
     return new ArrayList<String>(connectedNodes.keySet());
	}
	
	private int countMatches() {
		List<String> nodes = nodesConnected();
		int count = 0;
		for(String node :nodes) {
			if(node.matches(pattern)) {
				count = count+1;
			}
		}
		return count;
	}

	@Override
	public boolean should_ping() {
		if(minimum == -1) {
			return true;
		}
		int matches = countMatches();
		return matches < minimum;
	}

	@Override
	public void remoteNodeUp(String nodeName) {
		connectedNodes.put(nodeName, new Date());
	}

	@Override
	public void remoteNodeDown(String nodeName) {
		connectedNodes.remove(nodeName);
	}
	
}
