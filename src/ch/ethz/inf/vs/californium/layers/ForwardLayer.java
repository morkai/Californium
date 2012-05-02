package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.Message;

public abstract class ForwardLayer extends UpperLayer {
	
	public ForwardLayer() {
		super();
	}

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

	@Override
	protected void doReceiveMessage(Message msg) {
		LOG.finest(this.getClass().getSimpleName() + " doReceiveMessage");
		
		// pass message to registered receivers
		deliverMessage(msg);
	}
	
}