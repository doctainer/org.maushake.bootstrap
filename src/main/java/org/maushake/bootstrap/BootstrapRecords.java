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

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class BootstrapRecords {
	public static final String PING_STR = "ping";
	public static final String PONG_STR = "pong";
	public static final String BOOTSTRAP_STR = "bootstrap";
	public static final OtpErlangObject PING_ATOM = new OtpErlangAtom(PING_STR);
	public static final OtpErlangObject PONG_ATOM = new OtpErlangAtom(PONG_STR);
	public static final OtpErlangObject BOOTSTRAP_ATOM = new OtpErlangAtom(
			BOOTSTRAP_STR);

	public static OtpErlangObject bootstrap_ping(String nodeStr, InetAddress inetAddress) {
		OtpErlangObject node = new OtpErlangAtom(nodeStr);
		OtpErlangObject pingAddr = OtpErlangUtils.inet_ip_address(inetAddress);
		OtpErlangObject pingRecord = OtpErlangUtils.tuple(PING_ATOM, node,
				pingAddr);
		return bootstrap_record(pingRecord);
	}

	public static OtpErlangObject bootstrap_pong(String pongNodeStr,
			String pingNodeStr) {
		OtpErlangObject pongNode = new OtpErlangAtom(pongNodeStr);
		OtpErlangObject pingNode = new OtpErlangAtom(pingNodeStr);
		OtpErlangObject pongRecord = OtpErlangUtils.tuple(PONG_ATOM, pongNode,
				pingNode);
		return bootstrap_record(pongRecord);
	}

	public static OtpErlangObject bootstrap_record(OtpErlangObject arg) {
		return new OtpErlangTuple(new OtpErlangObject[] { BOOTSTRAP_ATOM, arg });
	}

	public static OtpErlangObject bootstrap_record_arg(OtpErlangObject theRecord) {
		OtpErlangObject[] elements = OtpErlangUtils.matchPair(theRecord);
		if (elements != null) {
			String recordName = OtpErlangUtils.matchRecord(elements);
			if (recordName != null && BOOTSTRAP_STR.equals(recordName)) {
				return elements[1];
			}
		}
		return null;
	}
}
