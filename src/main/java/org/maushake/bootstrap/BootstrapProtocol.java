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
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangBinary;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;

/**
 * An instance per node functions like bootsrap_protocol
 * 
 * @author m
 * 
 */
public class BootstrapProtocol implements Runnable {

	final private Logger logger = LoggerFactory.getLogger(BootstrapProtocol.class);
	// o5140624 remove some elements from bootstrap protocol state like pattern and min connections
	private static class State {

		// final String pattern;
		final Transport protocol;
		final int port; // the primary port
		final DatagramSocket socket; // generated by transport
		Timer timer;
		final long timeout; // the ping timeout
		// final int minimum;
		private final Logger logger;
		
		public State(BootstrapConfig bootstrapConfig) throws IOException {
			// pattern = bootstrapConfig.connect_regex();
			this.logger = LoggerFactory.getLogger(this.getClass());
			// System.out.println(this.getClass().getSimpleName()+".constructor(...)");
			Transport protocol_ = bootstrapConfig.getTransport();
			int port_ = bootstrapConfig.primary_port();
			socket = TransportUtils.open_socket(bootstrapConfig, port_, bootstrapConfig.secondary_posts());
			protocol = protocol_;
			port = port_;
			timeout = bootstrapConfig.ping_timeout();
			// minimum = bootstrapConfig.min_connections();
		}

		State setTimer(Timer timer) {
			this.timer = timer;
			return this;
		}
	};

	private interface MsgHandler<S> {
		S handleUdp(InetAddress fromIp, int fromPort, byte[] bytes, S state);

		S handleTimeout(Timer timerRef, S state);
	}

	/**
	 * a typed message box which is used by datagram receiver to queue received
	 * messages and by the protocol to queue timer events
	 * 
	 * @author jm
	 * 
	 */
	private static class MsgBox implements DatagramInbound, TimeoutEmitter {
		
		private static class QueuedRxDatagram {
			public final InetAddress fromIp;
			public final int fromPort;
			public final byte[] data;

			public QueuedRxDatagram(InetAddress fromIp, int port, byte[] data) {
				this.fromIp = fromIp;
				this.fromPort = port;
				this.data = data;
			}
		}

		private final GenericQueue<Object> q;
		private final Logger logger;
		
		public MsgBox() {
			this.q = new GenericQueue<Object>();
			this.logger = LoggerFactory.getLogger(this.getClass());
		}

		/**
		 * implements {@link DatagramInbound}
		 */
		@Override
		public void processDg(InetAddress fromIp, int fromPort, byte[] data) {
			if(logger.isTraceEnabled()) {
				logger.trace(".processDg fromIp = "+fromIp+" port = "+fromPort);
			}	

			// System.out.println(this.getClass().getSimpleName()+".processDg fromIp = "+fromIp+" port = "+fromPort);
			q.put(new QueuedRxDatagram(fromIp, fromPort, data));
		}

		/**
		 * <p>
		 * api method, called to receive one message. This method blocks until
		 * one message has been processed by the message handler
		 * </p>
		 */
		public <S> S receiveMsg(MsgHandler<S> msgHandler, S state) {
			Object msg = q.get();
			return applyMsgHandler(msgHandler, msg, state);
		}

		/**
		 * implements {@link TimeoutEmitter} api method to deliver sort of timeout
		 * event to this message box. The timer will be then passed to the
		 * MsgHandler.
		 * 
		 * @param timerRef
		 */
		@Override
		public void emit(Timer timerRef) {
			q.put(timerRef);
		}

		// corresponds to calling handle_info issued by gen_server
		private <S> S applyMsgHandler(MsgHandler<S> msgHandler, Object msg,
				S state) {
			if (msg instanceof Timer) {
				Timer timerRef = (Timer) msg;
				return msgHandler.handleTimeout(timerRef, state);
			}
			if (msg instanceof QueuedRxDatagram) {
				QueuedRxDatagram rxDg = (QueuedRxDatagram) msg;
				return msgHandler.handleUdp(rxDg.fromIp, rxDg.fromPort,
						rxDg.data, state);
			}
			throw new IllegalStateException();
		}
	}

	/**
	 * This class is named after the erlang handle_info callback function of
	 * bootstrap_protocol.erl. It contains all implementation which logically
	 * belongs to the handle_info function and its sub-functions.
	 * 
	 * @author jm
	 * 
	 */
	private static class HandleInfo implements MsgHandler<State> {
		Logger logger = LoggerFactory.getLogger(HandleInfo.class);
		private static enum BadDataKind {
		 EXTERNAL_FORMAT,
		 NOT_A_TUPLE,
		 NOT_A_BOOTSTRAP_RECORD,
		 NOT_A_PING_OR_PONG_RECORD,
		 NOT_A_PING_OR_PONG_RECORD_2,
		 NOT_A_PING_OR_PONG_RECORD_3,
		 NOT_INET_IP_ADDR,
		 NODE_NAME_NOT_ATOM,
		 NOT_SPECIFIED
		 
		};
		
		private final ConnectionController connectionController;
		private final String node; // corresponds to erlang:node()
		private BootstrapConfig bootstrapConfig;
		/**
		 * wraps the udp socket, so you can queue datagrams to be sent over the
		 * socket.
		 */
		private final DatagramOutbound outbound;
		/**
		 * <p>
		 * enables this handler to raise a timeout. This is a replacement for
		 * the erlang Pid self() with the associated message box into which the
		 * erlang bootstrap counterpart delivers a timeout event. The thread
		 * which runs this MsgHandler will then encounter the event and call the
		 * handleTimeout method
		 * </p>
		 * 
		 */
		private final TimeoutEmitter timeout;
		private final Random rn;
		
		public HandleInfo(String node, ConnectionController connectionController, DatagramOutbound outbound, TimeoutEmitter timeout, BootstrapConfig bootstrapConfig) {
			this.node = node;
			this.connectionController = connectionController;
			this.outbound = outbound;
			this.timeout = timeout;
			this.bootstrapConfig = bootstrapConfig;
			this.rn = new Random();
		}

		@Override
		public State handleUdp(InetAddress fromIp, int fromPort, byte[] bytes,
				State state) {
		    // System.out.println(this.getClass().getSimpleName()+".handleUdp fromIp = "+fromIp+" port = "+fromPort);
			OtpErlangBinary otpBinary = new OtpErlangBinary(bytes);
			OtpErlangObject term;
			try {
				term = OtpErlangUtils.binary_to_term(otpBinary);
				if(logger.isTraceEnabled()) {
				 logger.trace("handleUdp fromIp = "+fromIp+" port = "+fromPort+" msg = "+term);
				}
			} catch (IllegalArgumentException externalFormatStuff) {
				// System.out.println(this.getClass().getSimpleName()+".handleUdp Error has occurred");
				// externalFormatStuff.printStackTrace();
				return handleReceivedBadData(state, BadDataKind.EXTERNAL_FORMAT);
			}
			// match the term, expecting a bootstrap record
			if (!(term instanceof OtpErlangTuple)) {
				return handleReceivedBadData(state, BadDataKind.NOT_A_TUPLE);
			}
			OtpErlangObject pingOrPong = BootstrapRecords
					.bootstrap_record_arg(term);
			if (pingOrPong == null) {
				return handleReceivedBadData(state, BadDataKind.NOT_A_BOOTSTRAP_RECORD);
			}

			OtpErlangObject[] elements = OtpErlangUtils.matchTuple(pingOrPong);
			if (elements == null) {
				return handleReceivedBadData(state, BadDataKind.NOT_A_PING_OR_PONG_RECORD);
			}
			String shouldBePingOrPong = OtpErlangUtils.matchRecord(elements);
			if (shouldBePingOrPong == null) {
				return handleReceivedBadData(state, BadDataKind.NOT_A_PING_OR_PONG_RECORD_2);
			}
			if (BootstrapRecords.PING_STR.equals(shouldBePingOrPong)) {
				if (elements.length != 3) {
					return handleReceivedBadData(state, BadDataKind.NOT_A_PING_OR_PONG_RECORD_3);
				}
				String pingNodeStr = OtpErlangUtils.matchAtom(elements[1]);
				if (pingNodeStr == null) {
					return handleReceivedBadData(state, BadDataKind.NODE_NAME_NOT_ATOM);
				}
				InetAddress pingAddr = OtpErlangUtils
						.match_inet_ip_address(elements[2]);
				if (pingAddr == null) {
					// System.out.println(elements[2]);
					return handleReceivedBadData(state, BadDataKind.NOT_INET_IP_ADDR);
				}
				if (node.equals(pingNodeStr)) {
					return state;
				}
				return handle_ping(pingNodeStr, pingAddr, fromPort, state);
			}
			if (BootstrapRecords.PONG_STR.equals(shouldBePingOrPong)) {
				if (elements.length != 3) {
					return handleReceivedBadData(state, BadDataKind.NOT_SPECIFIED);
				}
				String pongNodeStr = OtpErlangUtils.matchAtom(elements[1]);
				if (pongNodeStr == null) {
					return handleReceivedBadData(state, BadDataKind.NOT_SPECIFIED);
				}
				String pingNodeStr = OtpErlangUtils.matchAtom(elements[1]);
				if (pingNodeStr == null) {
					return handleReceivedBadData(state, BadDataKind.NOT_SPECIFIED);
				}
				if (node.equals(pongNodeStr)) {
					return state;
				}
				return handle_pong(pongNodeStr, pingNodeStr, state);
			}
			return handleReceivedBadData(state, BadDataKind.NOT_SPECIFIED);
		}

		@Override
		public State handleTimeout(Timer timerRef, State state) {
			if(state.timer == timerRef) {
			  return timer_periodic(maybe_ping(state));
			}
			return state;
		}

		private State maybe_ping(State state) {
			if(connectionController.should_ping()) {
              try {
            	  return do_ping(node, state, bootstrapConfig, logger);	
              } catch(IOException ioex) {
            	  if(logger.isWarnEnabled()) {
            		  logger.warn("maybe_ping", ioex);
            	  }  
            	  return state;
              }
			}
			return state;
		}
		
		/**
		 * A malformed datagram has been received. 
		 * <pre>
		 *  o5140602 decons handle_info
		 *   handle "received bad data"
		 * </pre>
		 */
		private State handleReceivedBadData(State state, BadDataKind badDataKind) {
			if(logger.isWarnEnabled()) {
        		  logger.warn("!!!! received bad data: "+badDataKind);
        	}
			return state;
		}

		private State handle_ping(String pingNode, InetAddress pingAddr,
				int fromPort, State state) {
			// o5140603 bootstrap send DatagramPacket
			if(logger.isTraceEnabled()) {
				System.out.println("XXXXXXXXXXXXXXx");
				logger.trace("handle_ping pingAddr = "+pingAddr+" fromPort = "+fromPort);
			}
			OtpErlangObject otpObj = BootstrapRecords.bootstrap_pong(node,
					pingNode);
			byte[] dgData = OtpErlangUtils.term_to_binary(otpObj).binaryValue();
			outbound.deliverDg(new DatagramPacket(dgData, dgData.length,
					pingAddr, fromPort));
			if (fromPort != state.port) {
				outbound.deliverDg(new DatagramPacket(dgData, dgData.length,
						pingAddr, state.port));
			}
			return BootstrapProtocol.maybe_backoff(timeout, fromPort, pingNode, node, state);
		}

		/**
		 * 
		 * <pre>
		 *  o5140604 bootstrap handle_pong
		 * </pre>
		 */
		private State handle_pong(String pongNode, String pingNode, State state) {
			if(pingNode.equals(node)) {
				// pong in response of this having sent ping
				connectionController.maybe_connect(pongNode);
				return state;
			}
			connectionController.maybe_connect(pongNode);
			connectionController.maybe_connect(pingNode);
			return maybe_backoff(timeout, pingNode, node, state);
		}

		private State timer_periodic(State state) {
			long to = state.timeout;
			int rand_uni = rand_uniform(1000);
			long to_1000 = to - 1000;
			int max = to_1000 < 0 ? 0 : (int)to_1000;
			return start_timer(timeout, max+rand_uni, state);
		}
		
		private int rand_uniform(int b) {
			return rn.nextInt(b);
		}
	}

	/**
	 * the message box of this thread.
	 */
	private MsgBox msgBox;

	private final BootstrapConfig bootstrapConfig;
	private final String node;
	private final ConnectionController connectionController;
	private State init() throws IOException {
		// System.out.println(this.getClass().getSimpleName()+".init()");
		return timer_backoff(msgBox, new State(bootstrapConfig));
	}

	/**
	 * @param node
	 *            identifies unambiguously the participating node
	 */
	public BootstrapProtocol(String node, ConnectionController connectionController, BootstrapConfig bootstrapConfig) {
		this.node = node;
		this.connectionController = connectionController;
		this.bootstrapConfig = bootstrapConfig;
		this.msgBox = new MsgBox();
	}

	@Override
	public void run() {
		// System.out.println(this.getClass().getSimpleName()+".run()");
		if(logger.isTraceEnabled()) {
			logger.trace("Starting bootsrap protocol at node: "+node);
		}
		State state;
		try {
			state = init();
		} catch (IOException ioex) {
			System.out.println(this.getClass().getSimpleName()+".run() !!!+++ "+ioex.toString());
			return;
		}
		final DatagramSocketRunner socketRunnable = new DatagramSocketRunner(msgBox,
				state.socket);
		Thread mySocketThread = new Thread(socketRunnable, "Socket Thread("
				+ node + ')');
		mySocketThread.start();
		HandleInfo msgHandler = new HandleInfo(node, connectionController, socketRunnable, msgBox, bootstrapConfig);

		while (true) {
			State nextState = msgBox.receiveMsg(msgHandler, state);
			state = nextState;
		}
	}

	

	/**
	 * This function is called when this node receives a ping from another node.
	 * If this ping was sent from a node that listens on the primary port, the
	 * function delegates to maybe_backoff/2, in the other case this node backs
	 * off since the other node listens on a secondary port and thus can't
	 * receive pings at all, implication is that it must be the pinger.
	 */
	private static State maybe_backoff(TimeoutEmitter timeout, int fromPort,
			String fromNode, String thisNode, State state) {
		if (fromPort == state.port) {
			// came from primary port
			return maybe_backoff(timeout, fromNode, thisNode, state);
		}
		return timer_backoff(timeout, state);
	}

	/**
	 * <p>
	 * This function is called when ping clashes are detected (duplicate
	 * pinging). It determines which node is allowed to continue pinging (using
	 * a node name comparison). Ping clashes are either detected by handle_ping
	 * or handle_pong. If pongs containing a different ping node are received
	 * this means that there's another node pinging with the same source port.
	 * Only one pinger is allowed.
	 * </p>
	 */
	private static State maybe_backoff(TimeoutEmitter timeout, String fromNode,
			String thisNode, State state) {
		if (fromNode.compareTo(thisNode) < 0) { // fromNode < thisNode
			return timer_backoff(timeout, state);
		}
		return state;
	}

	private static State timer_backoff(TimeoutEmitter timeout, State state) {
		long t = state.timeout;
		long halfT = t >>> 1;
		long tComputed = t + halfT;
		long millis = tComputed < 1500 ? 1500 : tComputed;
		return start_timer(timeout, millis, state);
	}

	/**
	 * <p>
	 * Replan timeout to occur after a given number of milli secs
	 * named after bootstrap_protocol:start_timer
	 * </p>
	 */
	private static State start_timer(final TimeoutEmitter timeout, long millis,
			State state) {
		Timer current = state.timer;
		if (current != null) {
			current.cancel();
		}
		final Timer timerRef = new Timer();
		timerRef.schedule(new TimerTask() {
			@Override
			public void run() {
				timeout.emit(timerRef);
				// receiving of the timerRef will result in a ping if the
				// minimum number of connections is not reached so far
			}
		}, millis);
		return state.setTimer(timerRef);
	}

	private static State do_ping(String thisNode, State state, BootstrapConfig bootstrapConfig, Logger logger) throws IOException {
		return do_ping(thisNode, state, state.protocol.addresses(bootstrapConfig), logger);
	}
	
	private static State do_ping(String thisNode, State state, InetAddress[] addresses, Logger logger) {
		final boolean trace = logger.isTraceEnabled();
		if(trace) {
			logger.trace("do_ping: "+thisNode+" is going to send a ping datagram");
		}
		DatagramSocket socket = state.socket;
		int primaryPort = state.port;
		for (int i = 0, n = addresses.length; i < n; i++) {
			InetAddress multicastAddressGroup = addresses[i];
			byte[] bytes = consBOOTSTRAP_PING(thisNode, addresses[i]);

			DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
			packet.setAddress(multicastAddressGroup);
			packet.setPort(primaryPort);
			try {
				
				socket.send(packet);
				if(trace) {
					logger.trace("do_ping: ping datagram "+thisNode+" sent");
				}		
			} catch (IOException e) {
				if(logger.isWarnEnabled()) {
					logger.warn("do_ping", e);
				}
			}
		}
		return state;
	}
	
	/**
	 * Produce erlang/bootstrap compliant byte array for ping message to be sent
	 * over udp
	 * 
	 * <pre>
	 *  jm140512 Jinterface erlang marshalling 
	 *   adding term_to_binary
	 *   produce sth as follows:  
	 *   Msg = term_to_binary(?BOOTSTRAP_PING(node(), Addr)),
	 *   -define(BOOTSTRAP_PING(PingNode, PingAddr), {bootstrap, {ping, PingNode, PingAddr}}).
	 * </pre>
	 */
	private static byte[] consBOOTSTRAP_PING(String theNode,
			InetAddress pingAddr) {
		OtpErlangObject bootstrap_ping = BootstrapRecords.bootstrap_ping(
				theNode, pingAddr);
		OtpErlangBinary binary = OtpErlangUtils.term_to_binary(bootstrap_ping);
		return binary.binaryValue();
	}

		private static byte[] consBOOTSTRAP_PONG(String node, String pingNode) {
		OtpErlangObject bootstrap_pong = BootstrapRecords.bootstrap_pong(node,
				pingNode);
		OtpErlangBinary binary = OtpErlangUtils.term_to_binary(bootstrap_pong);
		return binary.binaryValue();
	}

	
}
