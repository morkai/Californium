/**
 * 
 */
package ch.ethz.inf.vs.californium.endpoint;

import java.net.SocketException;

import ch.ethz.inf.vs.californium.coap.Communicator;
import ch.ethz.inf.vs.californium.examples.resources.ProxyResource;
import ch.ethz.inf.vs.californium.examples.resources.RDResource;
import ch.ethz.inf.vs.californium.layers.ProxyStack;

/**
 * The class represent the container of the resources and the layers used by the proxy.
 * This class is a bridge between the standard Communicator and the modified ProxyCommunicator.
 * 
 * @author Francesco Corazza
 *
 */
public class ProxyEndpoint extends LocalEndpoint {
	
	//	private Set<Integer> directMessages = new HashSet<Integer>();
	private ProxyStack proxyStack;
	
	public ProxyEndpoint() throws SocketException {
		// initialize the Communicator with the default stack
		super();
		
		// initialize the proxyStack
		this.proxyStack = new ProxyStack();
		Communicator.getInstance().setLowerLayer(this.proxyStack);
		
		// add the proxying resource to manage the incoming request
		addResource(new ProxyResource());
		
		// add the resource directory resource
		addResource(new RDResource());
	}
	
	
	//	@Override
	//	public void visit(Request request) {
	//
	//		// Add additional handling like special logging here.
	//		System.out.println("PROXY-REQ");
	//		request.prettyPrint();
	//
	//		// check if the request has the proxy-uri option set
	//		if (RequestTranslator.isProxyUriSet(request)) {
	//			try {
	//				RequestTranslator.translateProxyUri(request);
	//				Communicator.getInstance().sendMessage(request);
	//				System.out.println("Request forwarded");
	//				request.prettyPrint();
	//			} catch (URISyntaxException e) {
	//				LOG.warning("Proxy-uri option malformed: " + e.getMessage());
	//				request.respond(CodeRegistry.RESP_BAD_OPTION); // TODO check the error
	//			} catch (IOException e) {
	//				LOG.warning("ProxyCommunicator error: " + e.getMessage());
	//				request.respond(CodeRegistry.RESP_INTERNAL_SERVER_ERROR); // TODO check the error
	//			}
	//		} else {
	//			// add the MID of the direct message to the map to recover it in the response
	//			this.directMessages.add(request.getMID());
	//
	//			// dispatch to requested resource as usual
	//			super.visit(request);
	//		}
	//	}
	//
	//	@Override
	//	public void visit(Response response) {
	//		System.out.println("PROXY-RESP");
	//		response.prettyPrint();
	//
	//		try {
	//
	//			if (this.directMessages.contains(response.getMID())) {
	//				this.directMessages.remove(response.getMID());
	//				Communicator.getInstance().sendMessage(response);
	//				System.out.println("Direct message");
	//			} else {
	//				Communicator.getInstance().sendMessage(
	//						response);
	//				System.out.println("Response forwarded");
	//			}
	//
	//		} catch (IOException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		//		super.handleResponse(response);
	//	}
	
	
}
