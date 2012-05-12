/**
 * 
 */
package ch.ethz.inf.vs.californium.layers.stacks;

import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.layers.CachingLayer;
import ch.ethz.inf.vs.californium.layers.MatchingLayer;
import ch.ethz.inf.vs.californium.layers.RateControlLayer;
import ch.ethz.inf.vs.californium.layers.TokenLayer;
import ch.ethz.inf.vs.californium.layers.TransactionLayer;
import ch.ethz.inf.vs.californium.layers.TransferLayer;
import ch.ethz.inf.vs.californium.layers.UDPLayer;


/**
 * @author Francesco Corazza
 *
 */
public class ProxyStack extends AbstractStack {
	
	private final Map<String, Integer> resourceMap = new ConcurrentHashMap<String, Integer>();
	private final Map<String, Integer> addressMap = new ConcurrentHashMap<String, Integer>();
	
	protected int requestPerSecond = 0;
	private int port;
	
	public ProxyStack(int udpPort, int transferBlockSize, boolean runAsDaemon,
			int requestPerSecond, ExecutorService threadPool)
					throws SocketException {
		super(udpPort, transferBlockSize, runAsDaemon, threadPool);
		this.requestPerSecond = requestPerSecond;
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
		enquequeLayer(new TokenLayer());
		enquequeLayer(new TransferLayer(this.transferBlockSize));
		enquequeLayer(new MatchingLayer());
		enquequeLayer(new TransactionLayer());
		enquequeLayer(new RateControlLayer(this.requestPerSecond));
		// enquequeLayer(new AdverseLayer());
		UDPLayer udpLayer = new UDPLayer(this.udpPort, this.runAsDaemon,
				"proxy");
		enquequeLayer(udpLayer);
		this.port = udpLayer.getPort();
	}
	
	@Override
	public int getPort() {
		return this.port;
	}
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		if (msg instanceof Request) {
			// update the proxy statistics
			updateStatistics((Request) msg);
		}
		
		super.doSendMessage(msg);
		//		System.out
		//		.println("ProxyStack - SENT MESSAGE TO LOWER LAYERS");
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {
		
		// send the message to the communicator
		//		try {
		//			Communicator.getInstance().sendMessage(msg);
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		
		super.doReceiveMessage(msg);
		//		System.out
		//		.println("ProxyStack - RECEIVED MESSAGE AND SENT TO COMMUNICATOR");
		
	}
	
	public String getStatistics() {
		StringBuilder builder = new StringBuilder();
		builder.append("Direct request for /proxy resource\n");
		
		// get/print the clients served
		builder.append("Addresses served: " + this.addressMap.size() + "\n");
		for (String key : this.addressMap.keySet()) {
			builder.append("Host: " + key + " requests: "
					+ this.addressMap.get(key) + " times\n");
		}
		
		// get/print the resources requested
		builder.append("Resources requested: " + this.resourceMap.size() + "\n");
		for (String key : this.resourceMap.keySet()) {
			builder.append("Resource " + key + " requested: "
					+ this.resourceMap.get(key) + " times\n");
		}
		
		return builder.toString();
	}
	
	/**
	 * @param request
	 */
	private void updateStatistics(Request request) {
		// get the keys to insert in the maps
		String addressString = request.getPeerAddress().toString();
		String resourceString = request.getUriPath();
		
		// get the count of request forwarded to the resource and from the specific address
		Integer resourceCount = this.resourceMap.get(resourceString);
		Integer addressCount = this.addressMap.get(addressString);
		
		// initialize the values
		if (resourceCount == null) {
			resourceCount = new Integer(0);
		}
		if (addressCount == null) {
			addressCount = new Integer(0);
		}
		
		// increment the counter
		resourceCount++;
		addressCount++;
		
		// add the count to the map
		this.resourceMap.put(resourceString, resourceCount);
		this.addressMap.put(addressString, addressCount);
		
	}
	
}
