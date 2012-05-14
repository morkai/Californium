/**
 * 
 */
package ch.ethz.inf.vs.californium.endpoint;

import java.io.IOException;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Communicator;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.CoapTranslator;

/**
 * @author Francesco Corazza
 *
 */
public class ProxyResource extends LocalResource {
	
	public ProxyResource() {
		super("proxy");
		setTitle("Forward the requests to a CoAP server");
		setResourceType("Proxy");
		isObservable(true); //TODO
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
		
		// forward the message iff the option proxy-uri is set
		if (CoapTranslator.isProxyUriSet(request)) {
			
			Request forwardedRequest = null;
			try {
				// create a new request to forward
				forwardedRequest = CoapTranslator
						.newRequestFromProxyUri(request);
				
				// enable response queue for synchronous I/O
				forwardedRequest.enableResponseQueue(true);
				
				// execute the request
				forwardedRequest.execute();
			} catch (URISyntaxException e) {
				LOG.warning("Proxy-uri option malformed: " + e.getMessage());
				//				msg.respond(CodeRegistry.RESP_BAD_OPTION); // TODO check the error
			} catch (IOException e) {
				System.err.println("Failed to execute request: "
						+ e.getMessage());
				//				System.exit(-1);
			}
			
			try {
				// receive the forwarded response
				Response forwardedResponse = forwardedRequest.receiveResponse();
				if (forwardedResponse != null) {
					// response received, create the real response for the request
					Response response = CoapTranslator
							.newResponseFromForwardedMessage(request,
									forwardedResponse);
					
					// complete the request
					request.respond(response);
				} else {
					// transaction timeout occurred
					System.out.println("No response received.");
					//TODO
				}
			} catch (InterruptedException e) {
				System.err.println("Receiving of response interrupted: "
						+ e.getMessage());
				//				System.exit(-1); TODO
			}
			// TODO check options of the response
			
		} else {
			// otherwise the request is directed to the resource /proxy for other purposes
			if (request instanceof GETRequest) {
				String payload = Communicator.getInstance()
						.getProxyingStatistics();
				request.respond(CodeRegistry.RESP_CONTENT, payload,
						MediaTypeRegistry.TEXT_PLAIN);
			} else {
				// other methods are not allowed
				request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
			}
		}
	}
}
