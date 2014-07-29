package org.maushake.bootstrap;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportUtils {
	static final Logger logger = LoggerFactory.getLogger(TransportUtils.class);
	// TODO: open_socket should allow for IOException
		public static DatagramSocket open_socket(BootstrapConfig bootstrapConfig, int primaryPort, int[] secondaryPorts) {
			if(logger.isTraceEnabled()) {
			 logger.trace("open_socket()");
			}
			Transport transport = bootstrapConfig.getTransport();
			DatagramSocket primarySocket = tryPortNumber(bootstrapConfig, transport, primaryPort);
			if(primarySocket != null) {
				return primarySocket;
			}
			// System.out.println(TransportUtils.class.getSimpleName()+".open_socket() not the primary port");
			if(logger.isTraceEnabled()) {
				 logger.trace("open_socket() not the primary port");
			}
			for (int portIdx = 0, n = secondaryPorts.length; portIdx < n; portIdx++) {
				DatagramSocket secondarySocket = tryPortNumber(bootstrapConfig, transport, secondaryPorts[portIdx]);
				if(secondarySocket != null) {
					return secondarySocket;
				}
			}
			throw new RuntimeException(new IOException("Too many nodes"));
		}
		
		/**
		 * create instance of DatagramSocket or nothing if the given port number is occupied
		 * @return instance of DatagramSocket or null if portNumber is occupied
		 */
		private static DatagramSocket tryPortNumber(BootstrapConfig bootstrapConfig, Transport transport, int portNumber) {
			try {
				return transport.openSocket(bootstrapConfig, portNumber);
			} catch (BindException bindex) {
				// @formatter:off
				// fall through
				// o5140628 provide example conntection contoller, add logging
				// Windows 7 seems not to work this way, so we never come here in this case
				// Windows 7 will throw java.net.SocketException: Unrecognized Windows Sockets error: 0: Cannot bind instead
				// @formatter:on
			} catch (IOException ioex) {
				if(logger.isTraceEnabled()) {
					  // the following will indicate that we tried to access a closed socket ???	
					  logger.trace("open_socket() !!! "+ioex.toString());
				}
				// System.out.println(TransportUtils.class.getSimpleName()+".open_socket() !!! "+ioex.toString());
				// @formatter:off
				// throw new RuntimeException(ioex);
				// @formatter:on
			}
			return null;
		}
}
