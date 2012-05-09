/**
 * 
 */
package ch.ethz.inf.vs.californium.layers.stacks;

import java.net.SocketException;



/**
 * @author Francesco Corazza
 *
 */
public class HttpClientStack extends AbstractStack {
	
	public HttpClientStack() throws SocketException {
		super();
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.layers.AbstractStack#createStack()
	 */
	@Override
	protected void createStack() throws SocketException {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.layers.AbstractStack#getPort()
	 */
	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
