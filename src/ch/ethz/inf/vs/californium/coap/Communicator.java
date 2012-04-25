/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.coap;

import java.io.IOException;
import java.net.SocketException;

import ch.ethz.inf.vs.californium.layers.AbstractStack;
import ch.ethz.inf.vs.californium.layers.DefaultStack;
import ch.ethz.inf.vs.californium.layers.ProxyStack;
import ch.ethz.inf.vs.californium.layers.UpperLayer;

/**
 * The class Communicator provides the message passing system and builds the
 * communication stack through which messages are sent and received. As a
 * subclass of {@link UpperLayer} it is actually a composite layer that contains
 * the subsequent layers in the order defined in {@link #buildStack()}.
 * <p>
 * Endpoints must register as a receiver using {@link #registerReceiver(MessageReceiver)}.
 * Prior to that, they should configure the Communicator using @link {@link #setup(int, boolean)}.
 * A client only using {@link Request}s are not required to do any of that.
 * Here, {@link Message}s will create the required instance automatically.
 * <p>
 * The Communicator implements the Singleton pattern, as there should only be
 * one stack per endpoint and it is required in different contexts to send a
 * message. It is not using the Enum approach because it still needs to inherit
 * from {@link UpperLayer}.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class Communicator extends UpperLayer {
	
	public static enum COMMUNICATOR_MODE {
		DEFAULT, COAP_PROXY, HTTP_TO_COAP_PROXY, COAP_TO_HTTP_PROXY;
		
	}
	
	private static COMMUNICATOR_MODE mode = COMMUNICATOR_MODE.DEFAULT;
	
	private volatile static Communicator singleton = null;
	
	private ProxyStack proxyStack;
	private DefaultStack defaultStack;
	
	// the following parameters should be externalized in an xml file
	// and set for each client/server created
	//	private static int serverHttpPort = 80;
	//	private static int clientHttpPort = 0;
	private static int defaultUdpPort = 0;
	private static int proxyUdpPort = 0;
	private static boolean daemon = false;
	private static int blockSize = 0;
	private static int requestPerSecond = 1000;
	
	
	public Communicator() {
		
		try {
			// the switch doesn't use the break because it needs an incremental initialization
			switch (mode) {
				case COAP_TO_HTTP_PROXY:
					//				this.httpClientStack = new HttpClientStack();
				case HTTP_TO_COAP_PROXY:
					//				this.httpServerStack = new HttpServerStack();
				case COAP_PROXY:
					this.proxyStack = new ProxyStack(proxyUdpPort, blockSize,
							daemon, requestPerSecond);
				case DEFAULT:
					this.defaultStack = new DefaultStack(defaultUdpPort,
							blockSize, daemon);
					setLowerLayer(this.defaultStack);
					break;
				default:
					LOG.severe("Not recognized option mode");
					break;
			}
		} catch (SocketException e) {
			LOG.severe("Unable to create the stack");
			//FIXME retrow an exception
		}
	}
	
	public static Communicator getInstance() {
		
		if (singleton==null) {
			synchronized (Communicator.class) {
				if (singleton==null) {
					singleton = new Communicator();
				}
			}
		}
		return singleton;
	}
	
	public static void setMode(COMMUNICATOR_MODE mode) {
		// double check
		if ((Communicator.mode != mode) && (singleton == null)) {
			synchronized (Communicator.class) {
				if (singleton == null) {
					Communicator.mode = mode;
					LOG.config("Setting mode communicator: " + mode);
					
				} else {
					LOG.severe("Communicator already initialized, setup failed");
				}
			}
		}
	}
	
	public static void setDefaultUdpPort(int port) {
		// double check
		if ((Communicator.defaultUdpPort != port) && (singleton == null)) {
			synchronized (Communicator.class) {
				if (singleton == null) {
					Communicator.defaultUdpPort = port;
					LOG.config("Setting port: " + port);
					
				} else {
					LOG.severe("Communicator already initialized, setup failed");
				}
			}
		}
	}
	
	public static void setDaemon(boolean daemon) {
		// double check
		if ((Communicator.daemon != daemon) && (singleton == null)) {
			synchronized (Communicator.class) {
				if (singleton == null) {
					Communicator.daemon = daemon;
					LOG.config("Setting daemon: " + daemon);
					
				} else {
					LOG.severe("Communicator already initialized, setup failed");
				}
			}
		}
	}
	
	public static void setBlockSize(int blockSize) {
		// double check
		if ((Communicator.blockSize != blockSize) && (singleton == null)) {
			synchronized (Communicator.class) {
				if (singleton == null) {
					Communicator.blockSize = blockSize;
					LOG.config("Setting blockSize: " + blockSize);
					
				} else {
					LOG.severe("Communicator already initialized, setup failed");
				}
			}
		}
	}
	
	public static void setRequestPerSecond(int requestPerSecond) {
		// double check
		if ((Communicator.requestPerSecond != requestPerSecond)
				&& (singleton == null)) {
			synchronized (Communicator.class) {
				if (singleton == null) {
					Communicator.requestPerSecond = requestPerSecond;
					LOG.config("Setting requestPerSecond: " + requestPerSecond);
					
				} else {
					LOG.severe("Communicator already initialized, setup failed");
				}
			}
		}
	}
	
	public static void setProxyUdpPort(int proxyUdpPort) {
		// double check
		if ((Communicator.proxyUdpPort != proxyUdpPort) && (singleton == null)) {
			synchronized (Communicator.class) {
				if (singleton == null) {
					Communicator.proxyUdpPort = proxyUdpPort;
					LOG.config("Setting proxyUdpPort: " + proxyUdpPort);
					
				} else {
					LOG.severe("Communicator already initialized, setup failed");
				}
			}
		}
	}
	
	// I/O implementation //////////////////////////////////////////////////////
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		// defensive programming before entering the stack, lower layers should assume a correct message.
		if (msg != null) {
			
			// check message before sending through the stack
			if (msg.getPeerAddress().getAddress()==null) {
				throw new IOException("Remote address not specified");
			}
			
			// delegate to first layer
			sendMessageOverLowerLayer(msg);
			
			//msg.prettyPrint();
		}
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {
		
		if (msg instanceof Response) {
			Response response = (Response) msg;
			
			// initiate custom response handling
			response.handle();
		}
		
		// pass message to registered receivers
		deliverMessage(msg);
		
		//msg.prettyPrint();
		
	}
	
	public AbstractStack getStackForMode(COMMUNICATOR_MODE mode) {
		AbstractStack result = null;
		
		switch (mode) {
			case COAP_TO_HTTP_PROXY:
				//				result = this.httpClientStack;
				break;
			case HTTP_TO_COAP_PROXY:
				//				result = this.httpServerStack;
				break;
			case COAP_PROXY:
				result = this.proxyStack;
				break;
			case DEFAULT:
				result = this.defaultStack;
				break;
			default:
				LOG.severe("Not recognized option mode");
				break;
		}
		
		return result;
	}
	
	public int getPort(COMMUNICATOR_MODE mode) {
		return getStackForMode(mode).getPort();
	}
	
}
