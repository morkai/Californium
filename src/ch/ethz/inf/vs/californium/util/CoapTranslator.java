/**
 * 
 */
package ch.ethz.inf.vs.californium.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Message.MessageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.layers.TransactionLayer;

/**
 * @author Francesco Corazza
 *
 */
public class CoapTranslator {
	
	protected static final Logger LOG = Logger
			.getLogger(CoapTranslator.class.getName());
	
	public static boolean isProxyUriSet(Message message) {
		if (message == null) {
			throw new IllegalArgumentException("message == null");
		}
		
		// check if the proxy-uri option is set or not
		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		return !message.getOptions(proxyUriOptNumber).isEmpty();
	}
	
	public static String getProxyUri(Message message) {
		if (message == null) {
			throw new IllegalArgumentException("message == null");
		}
		
		// get the proxy-uri option
		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		Option proxyUri = message.getOptions(proxyUriOptNumber).get(0);
		String proxyUriString = proxyUri.getStringValue();
		
		return proxyUriString;
	}
	
	public static Request newRequestFromProxyUri(Request request)
			throws URISyntaxException {
		if (request == null) {
			throw new IllegalArgumentException("request == null");
		}
		
		
		
		// create the new message
		Request result = null;
		try {
			result = request.getClass().newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the code from the message
		int code = request.getCode();
		result.setCode(code);
		
		// set type
		MessageType messageType = request.getType();
		result.setType(messageType);
		
		// copy payload
		byte[] payload = request.getPayload();
		result.setPayload(payload);
		
		// set a new MID
		int mid = TransactionLayer.nextMessageID();
		result.setMID(mid);
		
		// set the address from the proxy-uri option
		String proxyUriString = getProxyUri(request);
		URI serverUri = new URI(proxyUriString);
		result.setURI(serverUri);
		
		// copy every option but the proxy-uri
		for (Option option : request.getOptions()) {
			if ((option.getOptionNumber() != OptionNumberRegistry.PROXY_URI)
					&& (option.getOptionNumber() != OptionNumberRegistry.URI_PATH)) {
				result.setOption(option);
			}
		}
		
		// insert the forwarded message into the map
		//		forwardedMessageMap.put(result.getPeerAddress().toString(), request);
		
		return result;
	}
	
	public static Response newResponseFromForwardedMessage(
			Request forwardedRequest,
			Response forwardedResponse) {
		if (forwardedResponse == null) {
			throw new IllegalArgumentException("forwardedResponse == null");
		}
		
		Response result = null;
		
		// create the new message
		try {
			result = forwardedResponse.getClass().newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the code from the message
		int code = forwardedResponse.getCode();
		result.setCode(code);
		
		// set the type
		MessageType messageType = forwardedResponse.getType();
		result.setType(messageType);
		
		// copy payload
		byte[] payload = forwardedResponse.getPayload();
		result.setPayload(payload);
		
		// copy MID
		int originalMid = forwardedRequest.getMID(); // TODO check
		result.setMID(originalMid);
		
		// set the address from the proxy-uri option
		EndpointAddress peerAddress = forwardedRequest.getPeerAddress();
		result.setPeerAddress(peerAddress);
		
		// copy every option
		for (Option option : forwardedResponse.getOptions()) {
			result.setOption(option);
		}
		
		return result;
	}
}
