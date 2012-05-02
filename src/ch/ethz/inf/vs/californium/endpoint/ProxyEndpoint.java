/**
 * 
 */
package ch.ethz.inf.vs.californium.endpoint;

import java.net.SocketException;

import ch.ethz.inf.vs.californium.coap.Communicator;
import ch.ethz.inf.vs.californium.coap.Communicator.COMMUNICATOR_MODE;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class represent the container of the resources and the layers used by the proxy.
 * This class is a bridge between the standard Communicator and the modified ProxyCommunicator.
 * 
 * @author Francesco Corazza
 *
 */
public class ProxyEndpoint extends LocalEndpoint {
	
	public ProxyEndpoint(int serverUdpPort, int defaultBlockSze,
			boolean daemon, int requestPerSecond, int clientUdpPort)
					throws SocketException {
		super(true); // the boolean is only a placeholder to choose a different constructor from LocalEndpoint
		// TODO the constructor must not call super()
		
		this.mode = COMMUNICATOR_MODE.COAP_PROXY;
		
		// initialize communicator and register the endpoint as a receiver
		Communicator.setMode(this.mode);
		Communicator.setDefaultUdpPort(serverUdpPort);
		Communicator.setBlockSize(defaultBlockSze);
		Communicator.setDaemon(daemon);
		Communicator.setProxyUdpPort(clientUdpPort);
		Communicator.setRequestPerSecond(requestPerSecond);
		
		Communicator.getInstance().registerReceiver(this);
		
		// add the proxying resource to manage the incoming request
		addResource(new ProxyResource());
		
		// add the resource directory resource
		addResource(new RDResource());
	}
	
	public ProxyEndpoint() throws SocketException {
		this(Properties.std.getInt("DEFAULT_PORT"), 0, false, 0, 0);
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
