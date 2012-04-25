/**
 * 
 */
package ch.ethz.inf.vs.californium.layers;

import java.net.SocketException;


/**
 * @author Francesco Corazza
 *
 */
public class ProxyStack extends AbstractStack {
	
	protected int requestPerSecond = 10;
	
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
		this.actualPort = udpLayer.getPort();
	}
	
}
