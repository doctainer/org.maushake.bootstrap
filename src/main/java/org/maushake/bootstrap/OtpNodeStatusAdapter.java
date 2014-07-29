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

import com.ericsson.otp.erlang.OtpNodeStatus;

/**
 * 
 * {@link OtpNodeStatus} is a class and not an interface which can be implemented.
 * This adapter takes an interface and delegates 
 * all callbacks to that interface
 * 
 * <pre>
 *  o5140626 provide howto for java bootstrap  
 * </pre>
 *
 */
public class OtpNodeStatusAdapter extends OtpNodeStatus {
	private final OtpNodeUpDownListener otpNodeUpDownListener; 

	public OtpNodeStatusAdapter(OtpNodeUpDownListener otpNodeUpDownListener) {
	 this.otpNodeUpDownListener = otpNodeUpDownListener;	
	}
	
	/**
	 * overrides {@link OtpNodeStatus}
	 */
	@Override
	public void remoteStatus(final String node, final boolean up,
			final Object info) {
		// with info being null when up is true
		// or info being some Exception otherwise
		if (up) {
			otpNodeUpDownListener.remoteNodeUp(node);
		} else {
			otpNodeUpDownListener.remoteNodeDown(node);
		}
	}
}
