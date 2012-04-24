/**
 * 
 */
package ch.ethz.inf.vs.californium.http;

import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;

/**
 * @author Francesco Corazza
 *
 */
public class RequestTranslator {
	
	public static boolean isProxyUriSet(Request request) {
		// check if the proxy-uri option is set or not
		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		return !request.getOptions(proxyUriOptNumber).isEmpty();
	}
	
	public static Request translateProxyUri(Request request)
			throws URISyntaxException {
		// get the proxy-uri option
		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		Option proxyUri = request.getOptions(proxyUriOptNumber).get(0);
		String proxyUriString = proxyUri.getStringValue();
		
		// TODO test the string with multiple regexp
		
		URI serverUri = new URI(proxyUriString);
		//		System.out.println("string: " + proxyUriString);
		//		System.out.println("uri: " + serverUri);
		
		// modify the request to translate the proxy-uri option in: uri-host, uri-port, uri-path, uri-query
		synchronized (request) { // TODO check sync
			// delete proxy-uri option
			request.removeOption(proxyUriOptNumber);
			
			// set the new URI
			request.setURI(serverUri);
			
		}
		
		System.out.println("REQUEST TRANSLATED:");
		request.prettyPrint();
		
		return request;
	}
	
}
