/**
 * 
 */
package ch.ethz.inf.vs.californium.examples.resources;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

/**
 * @author Francesco Corazza
 *
 */
public class ProxyResource extends LocalResource {
	public ProxyResource() {
		super("proxy");
		setTitle("Forward the requests to a CoAP server");
		setResourceType("Proxy");
		isObservable(false);
	}
	
	@Override
	public void performGET(GETRequest request) {
		
		// the request is directed to the proxy resource of the proxy and the requester is not asking to forward the request
		request.respond(CodeRegistry.RESP_CONTENT,
				"Direct request for /proxy resource");
	}
	
	@Override
	public void performPUT(PUTRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}
	
	@Override
	public void performPOST(POSTRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}
	
	@Override
	public void performDELETE(DELETERequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}
	
}
