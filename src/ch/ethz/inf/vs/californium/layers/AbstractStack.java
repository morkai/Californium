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
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.net.SocketException;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import ch.ethz.inf.vs.californium.coap.Message;

/**
 * The Class AbstractCommunicator.
 *
 * @author Francesco Corazza
 */
public abstract class AbstractStack extends UpperLayer {
	
	// Static Attributes ///////////////////////////////////////////////////////////
	
	/** The udp port. */
	protected int udpPort = 0;
	
	/** The run as daemon. */
	protected boolean runAsDaemon = true; // JVM will shut down if no user threads are running
	
	/** The transfer block size. */
	protected int transferBlockSize = 0;
	
	// Members /////////////////////////////////////////////////////////////////////
	
	/** The layer queue. */
	private final Deque<Layer> layerQueue = new LinkedBlockingDeque<Layer>();
	
	protected UpperLayer upperLayer = null;
	
	// Constructors /////////////////////////////////////////////////////////////////////
	
	public AbstractStack(int udpPort, int transferBlockSize, boolean runAsDaemon)
			throws SocketException {
		enquequeLayer(this);
		createStack();
	}
	
	protected abstract void createStack() throws SocketException;
	
	public AbstractStack(int udpPort, boolean runAsDaemon)
			throws SocketException {
		this(udpPort, 0, runAsDaemon);
	}
	
	public AbstractStack(boolean runAsDaemon) throws SocketException {
		this(0, runAsDaemon);
	}
	
	public AbstractStack(int udpPort) throws SocketException {
		this(udpPort, true);
	}
	
	public AbstractStack() throws SocketException {
		this(0);
	}
	
	
	/**
	 * Sets the up port.
	 * 
	 * @param port
	 *            the new up port
	 */
	public void setupPort(int port) {
		this.udpPort = port;
		LOG.config(String.format("Custom port: %d", this.udpPort));
	}
	
	/**
	 * Sets the up transfer.
	 * 
	 * @param defaultBlockSize
	 *            the new up transfer
	 */
	public void setupTransfer(int defaultBlockSize) {
		this.transferBlockSize = defaultBlockSize;
		LOG.config(String.format("Custom block size: %d", this.transferBlockSize));
	}
	
	/**
	 * Sets the up deamon.
	 * 
	 * @param daemon
	 *            the new up deamon
	 */
	public void setupDeamon(boolean daemon) {
		this.runAsDaemon = daemon;
		LOG.config(String.format("Custom daemon option: %b", this.runAsDaemon));
	}
	
	/**
	 * @return the upperLayer
	 */
	public UpperLayer getUpperLayer() {
		return this.upperLayer;
	}
	
	/**
	 * @param upperLayer the upperLayer to set
	 */
	public void setUpperLayer(UpperLayer upperLayer) {
		if (upperLayer == null) {
			throw new IllegalArgumentException("upperLayer == null");
		}
		
		// if the upperLayer is already set it needs to remove it from the deque
		if (this.upperLayer != null) {
			this.layerQueue.remove(upperLayer);
		}
		
		// put the layer at the beginnign of the deque and link it it with the communicator
		this.layerQueue.addLast(upperLayer);
		upperLayer.setLowerLayer(this);
		this.upperLayer = upperLayer;
	}
	
	// Internal ////////////////////////////////////////////////////////////////
	
	protected synchronized void enquequeLayer(Layer layer) {
		// get the first layer if the stack is not empty
		if (!this.layerQueue.isEmpty()) {
			Layer firstLayer = this.layerQueue.peekFirst();
			
			// link the upper layer in the stack with the inserting layer
			if (firstLayer instanceof UpperLayer) {
				((UpperLayer) firstLayer).setLowerLayer(layer);
				LOG.config(firstLayer.getClass().getSimpleName()
						+ " has above "
						+ layer.getClass().getSimpleName());
			}
		}
		
		this.layerQueue.addFirst(layer);
	}
	
	public void pushMessageInTheStack(Message message) throws IOException {
		LOG.fine(this.getClass().getSimpleName() + " pushMessageInTheStack");
		
		this.upperLayer.sendMessage(message);
		
		//message.acceptVisitor(this.visitor);
	}
	
	// I/O implementation //////////////////////////////////////////////////////
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.inf.vs.californium.layers.Layer#doSendMessage(ch.ethz.inf.vs.
	 * californium.coap.Message)
	 */
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		LOG.finest(this.getClass().getSimpleName() + " doSendMessage");
		
		// defensive programming before entering the stack, lower layers should assume a correct message.
		if (msg != null) {
			
			// check message before sending through the stack
			if (msg.getPeerAddress().getAddress() == null) {
				throw new IOException("Remote address not specified");
			}
			
			// delegate to first layer
			sendMessageOverLowerLayer(msg);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.inf.vs.californium.layers.Layer#doReceiveMessage(ch.ethz.inf.
	 * vs.californium.coap.Message)
	 */
	@Override
	protected void doReceiveMessage(Message msg) {
		LOG.finest(this.getClass().getSimpleName() + " doReceiveMessage");
		
		// pass message to registered receivers
		deliverMessage(msg);
	}
	
}
