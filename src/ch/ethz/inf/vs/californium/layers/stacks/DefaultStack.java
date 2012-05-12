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
package ch.ethz.inf.vs.californium.layers.stacks;

import java.net.SocketException;
import java.util.concurrent.ExecutorService;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.MessageReceiver;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.layers.MatchingLayer;
import ch.ethz.inf.vs.californium.layers.TokenLayer;
import ch.ethz.inf.vs.californium.layers.TransactionLayer;
import ch.ethz.inf.vs.californium.layers.TransferLayer;
import ch.ethz.inf.vs.californium.layers.UDPLayer;
import ch.ethz.inf.vs.californium.layers.UpperLayer;

/**
 * The class DefaultStack provides the message passing system and builds the
 * communication stack through which messages are sent and received. As a
 * subclass of {@link UpperLayer} it is actually a composite layer that contains
 * the subsequent layers in the order defined in {@link #buildStack()}.
 * <p>
 * Endpoints must register as a receiver using
 * {@link #registerReceiver(MessageReceiver)}. Prior to that, they should
 * configure the DefaultStack using {@link #setup(int, boolean)}. A client only
 * using {@link Request}s are not required to do any of that. Here,
 * {@link Message}s will create the required instance automatically.
 * <p>
 * The DefaultStack implements the Singleton pattern, as there should only be
 * one stack per endpoint and it is required in different contexts to send a
 * message. It is not using the Enum approach because it still needs to inherit
 * from {@link UpperLayer}.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class DefaultStack extends AbstractStack {
	
	private int port;
	
	public DefaultStack(int udpPort, int transferBlockSize,
			boolean runAsDaemon, ExecutorService threadPool)
					throws SocketException {
		super(udpPort, transferBlockSize, runAsDaemon, threadPool);
	}
	
	public DefaultStack(int udpPort, boolean runAsDaemon)
			throws SocketException {
		super(udpPort, runAsDaemon);
	}
	
	public DefaultStack(boolean runAsDaemon) throws SocketException {
		super(runAsDaemon);
	}
	
	public DefaultStack(int udpPort) throws SocketException {
		super(udpPort);
	}
	
	public DefaultStack() throws SocketException {
		super();
	}
	
	@Override
	protected void createStack() throws SocketException {
		// initialize layers
		enquequeLayer(new TokenLayer());
		enquequeLayer(new TransferLayer(this.transferBlockSize));
		enquequeLayer(new MatchingLayer());
		enquequeLayer(new TransactionLayer());
		// enquequeLayer(new AdverseLayer());
		UDPLayer udpLayer = new UDPLayer(this.udpPort, this.runAsDaemon);
		enquequeLayer(udpLayer);
		this.port = udpLayer.getPort();
	}
	
	@Override
	public int getPort() {
		return this.port;
	}
	
}
