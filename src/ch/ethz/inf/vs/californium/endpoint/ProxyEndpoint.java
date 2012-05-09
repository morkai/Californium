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
			boolean daemon, int requestPerSecond, int httpPort)
					throws SocketException {
		super(true); // the boolean is only a placeholder to choose a different constructor from LocalEndpoint
		// TODO the constructor must not call super()
		
		this.mode = COMMUNICATOR_MODE.HTTP_TO_COAP_PROXY;
		
		// set the parameters of the communicator
		Communicator.setMode(this.mode);
		Communicator.setDefaultUdpPort(serverUdpPort);
		Communicator.setHttpPort(httpPort);
		Communicator.setBlockSize(defaultBlockSze);
		Communicator.setDaemon(daemon);
		Communicator.setRequestPerSecond(requestPerSecond);
		
		// initialize communicator and register the endpoint as a receiver
		Communicator.getInstance().registerReceiver(this);
		
		// add the proxying resource to manage the incoming request
		addResource(new ProxyResource());
		
		// add the resource directory resource
		addResource(new RDResource());
	}
	
	public ProxyEndpoint() throws SocketException {
		this(Properties.std.getInt("DEFAULT_PORT"), 0, false, 0, Properties.std
				.getInt("HTTP_PORT"));
	}
	
}
