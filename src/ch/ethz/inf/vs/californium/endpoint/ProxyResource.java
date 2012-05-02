/**
 * 
 */
package ch.ethz.inf.vs.californium.endpoint;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Communicator;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.http.MessageTranslator;

/**
 * @author Francesco Corazza
 *
 */
public class ProxyResource extends LocalResource {
	
	private final Map<String, Integer> resourceMap = new HashMap<String, Integer>();
	private final Map<String, Integer> addressMap = new HashMap<String, Integer>();
	
	public ProxyResource() {
		super("proxy");
		setTitle("Forward the requests to a CoAP server");
		setResourceType("Proxy");
		isObservable(false);
	}
	
	@Override
	public void performGET(GETRequest request) {
		handleRequest(request);
	}
	
	@Override
	public void performPUT(PUTRequest request) {
		handleRequest(request);
	}
	
	@Override
	public void performPOST(POSTRequest request) {
		handleRequest(request);
	}
	
	@Override
	public void performDELETE(DELETERequest request) {
		handleRequest(request);
	}
	
	/**
	 * The method check if the proxy-uri is set and forwards the request to the default stack if not set and to the proxy stack if set
	 * 
	 * @param request
	 */
	private void handleRequest(Request request) {
		// forward only iff the option proxy-uri is set
		if (MessageTranslator.isProxyUriSet(request)) {
			
			setStats(request);
			
			// call the communicator to forward the request to the proxy stack
			Communicator.getInstance().sendMessageOverProxy(request);
			System.out
			.println("PROXY RESOURCE - REQUEST FORWARDED TO PROXY STACK");
		} else {
			// otherwise the request is directed to the resource /proxy for other purposes
			String payload = getStats(request);
			request.respond(CodeRegistry.RESP_CONTENT,
					payload);
		}
	}
	
	/**
	 * @param request
	 * @return
	 */
	private String getStats(Request request) {
		StringBuilder builder = new StringBuilder();
		builder.append("Direct " + CodeRegistry.toString(request.getCode())
				+ " request for /proxy resource\n");
		builder.append("Addresses served: " + this.addressMap.size() + "\n");
		for (String key : this.addressMap.keySet()) {
			builder.append("Host: " + key + " requests: "
					+ this.addressMap.get(key)
					+ " times\n");
		}
		builder.append("Resources requested: " + this.resourceMap.size() + "\n");
		for (String key : this.resourceMap.keySet()) {
			builder.append("Resource " + key + " requested: "
					+ this.resourceMap.get(key)
					+ " times\n");
		}
		return builder.toString();
	}
	
	/**
	 * @param request
	 */
	private void setStats(Request request) {
		String addressString = request.getPeerAddress().toString();
		String resourceString = MessageTranslator.getProxyUri(request);
		
		// get the count of request forwarded to the resource and from the specific address
		Integer resourceCount = this.resourceMap.get(resourceString);
		Integer addressCount = this.addressMap.get(addressString);
		
		// initialize the value
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
		this.resourceMap
		.put(resourceString, resourceCount);
		this.addressMap.put(addressString, addressCount);
		
	}
}
