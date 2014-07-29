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
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangBinary;
import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangInt;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpExternal;
import com.ericsson.otp.erlang.OtpInputStream;
import com.ericsson.otp.erlang.OtpOutputStream;

/**
 * PoM org.orgolo.jinterface
 * 
 * @author mausi
 * 
 */
public class OtpErlangUtils {
	/**
	 * implements the java version of erlang's term_to_binary<br>
	 * <br>
	 * Erlang:
	 * 
	 * <pre>
	 * 3> term_to_binary({1,2}).
	 * <<131,104,2,97,1,97,2>>
	 * 4>
	 * </pre>
	 * 
	 * <pre>
	 *  jm140512 Jinterface erlang marshalling
	 * </pre>
	 */
	public static OtpErlangBinary term_to_binary(OtpErlangObject term) {
		OtpOutputStream os = new OtpOutputStream();
		os.write1(OtpExternal.versionTag);
		os.write_any(term);
		try {
			os.close();
		} catch (IOException e) {
		}
		return new OtpErlangBinary(os.toByteArray());
	}

	/**
	 * implements the java version of erlang's binary_to_term<br>
	 * 
	 * <pre>
	 *  jm140602 Jinterface need binary_to_term when reading udp
	 * </pre>
	 */
	public static OtpErlangObject binary_to_term(final OtpErlangBinary otpBinary) {
		final OtpInputStream ois = new OtpInputStream(otpBinary.binaryValue());
		try {
			return ois.read_any();
		} catch (OtpErlangDecodeException dE) {
			throw new IllegalArgumentException(dE);
		} finally {
			try {
				ois.close();
			} catch (final IOException e) {
			}
		}
	}

	// Unfortunately, jinterface OtpErlangBinary lacks a helpful toString method
	public static String toString(OtpErlangBinary binary) {
		byte[] bytes = binary.binaryValue();
		StringBuilder sb = new StringBuilder();
		sb.append("<<");
		for (int i = 0, n = bytes.length; i < n; i++) {
			if (i != 0) {
				sb.append(',');
			}
			sb.append(0xff & bytes[i]);
		}
		sb.append(">>");
		return sb.toString();
	}

	public static OtpErlangTuple tuple(OtpErlangObject... elems) {
		return new OtpErlangTuple(elems);
	}
    
	// convert to type inet:ip_address()
	public static OtpErlangObject inet_ip_address(InetAddress inetAddress) {
		byte[] rawAddr = inetAddress.getAddress();
		int n = rawAddr.length;
		OtpErlangInt[] eInts;
		if (n == 4) {
			eInts = new OtpErlangInt[4];
			for (int i = 0; i < n; i++) {
				eInts[i] = new OtpErlangInt(rawAddr[i] & 0xff);
			}

		} else {
			// expecting 16 bytes
			eInts = new OtpErlangInt[8];
			int p = 0;
			for (int i = 0; i < 8; i++) {
				int b_hi = rawAddr[p++] & 0xff;
				int b_lo = rawAddr[p++] & 0xff;
				eInts[i] = new OtpErlangInt((b_hi << 8) | b_lo);
			}
		}
		return new OtpErlangTuple(eInts);
	}

	/**
	 * convert type inet:ip_address() back to InetAddress
	 * <pre>
	 *  jm140625 inet_ip_address seems to have longs
	 * </pre>
	 */
	public static InetAddress match_inet_ip_address(OtpErlangObject otpObj) {
		OtpErlangObject[] elements = matchTuple(otpObj);
		int len = elements.length;
		byte[] addr = null;
		if (len == 4) {
			// ip4_address() = {0..255, 0..255, 0..255, 0..255}
			addr = new byte[4];
			for (int i = 0; i < 4; i++) {
				// jm140625 inet_ip_address seems to have longs
				OtpErlangObject element = elements[i];
				int k;
				try {
					if (element instanceof OtpErlangLong) {
						k = ((OtpErlangLong) element).intValue();
					} else {
						if (element instanceof OtpErlangInt) {
							k = ((OtpErlangInt) element).intValue();
						} else {
							return null;
						}
					}
				} catch (OtpErlangRangeException e) {
					return null;
				}
				if (256 <= k) {
					return null;
				}
				addr[i] = (byte) k;
			}
			try {
				return InetAddress.getByAddress(addr);
			} catch (UnknownHostException e) {
				// from InetAddress.getByAddress documentation:
				// UnknownHostException - if IP address is of illegal length
				throw new RuntimeException(e);
			}
		}
		if (len == 8) {
			// @formatter:off
			// don't build the literal IPv6 address format defined in RFC 2373
			// ip6_address() =
			// {0..65535,
			// 0..65535,
			// 0..65535,
			// 0..65535,
			// 0..65535,
			// 0..65535,
			// 0..65535,
			// 0..65535}
			// @formatter:on
			addr = new byte[16];
			int p = 0;
			for (int i = 0; i < 8; i++) {
				OtpErlangObject element = elements[i];
				int k;
				try {
					if (element instanceof OtpErlangLong) {
						k = ((OtpErlangLong) element).intValue();
					} else {
						if (element instanceof OtpErlangInt) {
							k = ((OtpErlangInt) element).intValue();
						} else {
							return null;
						}
					}
				} catch (OtpErlangRangeException e) {
					return null;
				}
				if (0x10000 <= k) {
					return null;
				}
				addr[p++] = (byte) (k >> 8);
				addr[p++] = (byte) (k & 0xff);
			}

		}
		if (addr == null) {
			return null;
		}
		try {
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			// from InetAddress.getByAddress documentation:
			// UnknownHostException - if IP address is of illegal length
			throw new RuntimeException(e);
		}
	}

	/**
	 * check if tuple, return elements if so
	 */
	public static OtpErlangObject[] matchTuple(OtpErlangObject otpObj) {
		return otpObj instanceof OtpErlangTuple ? ((OtpErlangTuple) otpObj)
				.elements() : null;
	}

	public static OtpErlangObject[] matchPair(OtpErlangObject otpObj) {
		OtpErlangObject[] elements = matchTuple(otpObj);
		return elements.length == 2 ? elements : null;
	}

	/**
	 * check if first element is an atom and if so return the atomValue
	 * 
	 * @return name of the record or null elements do not represent an erlang
	 *         record
	 */
	public static String matchRecord(OtpErlangObject[] elements) {
		if (elements.length != 0) {
			return matchAtom(elements[0]);
		}
		return null;
	}

	public static String matchAtom(OtpErlangObject otpObj) {
		return otpObj instanceof OtpErlangAtom ? ((OtpErlangAtom) otpObj)
				.atomValue() : null;
	}

}
