/**
 * 
 */
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.inf.vs.californium.coap.Communicator;
import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.http.MessageTranslator;


/**
 * @author Francesco Corazza
 *
 */
public class ProxyStack extends AbstractStack {
	
	private Map<String, EndpointAddress> forwardMap = new ConcurrentHashMap<String, EndpointAddress>();
	protected int requestPerSecond = 10;
	private int port;
	
	public ProxyStack(int udpPort, int transferBlockSize, boolean runAsDaemon,
			int requestPerSecond) throws SocketException {
		super(udpPort, transferBlockSize, runAsDaemon);
		this.requestPerSecond = requestPerSecond;
	}
	
	public ProxyStack(int udpPort, int transferBlockSize, boolean runAsDaemon)
			throws SocketException {
		super(udpPort, transferBlockSize, runAsDaemon);
	}
	
	public ProxyStack(int udpPort, boolean runAsDaemon) throws SocketException {
		super(udpPort, runAsDaemon);
	}
	
	public ProxyStack(int udpPort) throws SocketException {
		super(udpPort);
	}
	
	public ProxyStack() throws SocketException {
		super();
	}
	
	@Override
	protected void createStack() throws SocketException {
		// initialize layers and the stack
		enquequeLayer(new CachingLayer());
		enquequeLayer(new RateControlLayer(this.requestPerSecond));
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
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		try {
			//			EndpointAddress sourceAddress = msg.getPeerAddress();
			//			RequestTranslator.changeDestinationFromProxyUri(msg);
			//			EndpointAddress destinationAddress = msg.getPeerAddress();
			//
			//			this.forwardMap.put(destinationAddress.toString(), sourceAddress);
			
			// TODO check options of the requests
			
			// if the message has the proxy-uri option set, send a new message
			//			msg.prettyPrint();
			if (MessageTranslator.isProxyUriSet(msg)) {
				msg = MessageTranslator.newMessageFromProxyUri(msg);
			}
			//			msg.prettyPrint();
		} catch (URISyntaxException e) {
			LOG.warning("Proxy-uri option malformed: " + e.getMessage());
			//					msg.respond(CodeRegistry.RESP_BAD_OPTION); // TODO check the error
		}
		
		super.doSendMessage(msg);
		
		System.out
		.println("ProxyStack - SENT MESSAGE TO LOWER LAYERS");
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {
		
		// get the forwardedMessage if it was forwarded
		//		msg.prettyPrint();
		Message forwardedMessage = MessageTranslator.newMessageFromForwardedMessage(msg);
		if (forwardedMessage != null) {
			msg = forwardedMessage;
		}
		//		msg.prettyPrint();
		// get the destination from the map
		//		EndpointAddress destinationAddress = msg.getPeerAddress();
		//
		//		// Retrieve the source message from the map
		//		EndpointAddress sourceAddress = this.forwardMap.get(destinationAddress
		//				.toString());
		//
		//		// modify the message
		//		msg.setPeerAddress(sourceAddress);
		
		try {
			Communicator.getInstance().sendMessage(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//		super.doReceiveMessage(msg);
		
		System.out
		.println("ProxyStack - RECEIVED MESSAGE AND SENT TO DEFAULT STACK");
		
	}
	
}
