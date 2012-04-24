/**
 * 
 */
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.Message;

/**
 * @author Francesco Corazza
 *
 */
public class CachingLayer extends Layer {
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.layers.Layer#doSendMessage(ch.ethz.inf.vs.californium.coap.Message)
	 */
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.layers.Layer#doReceiveMessage(ch.ethz.inf.vs.californium.coap.Message)
	 */
	@Override
	protected void doReceiveMessage(Message msg) {
		// TODO Auto-generated method stub
		
	}
	
}
