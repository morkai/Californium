/**
 * 
 */
package ch.ethz.inf.vs.californium.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Message.MessageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.layers.TransactionLayer;

/**
 * @author Francesco Corazza
 *
 */
public class MessageTranslator {
	
	private final static ConcurrentHashMap<String, Message> forwardedMessageMap = new ConcurrentHashMap<String, Message>();
	
	protected static final Logger LOG = Logger
			.getLogger(MessageTranslator.class.getName());
	
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
	
	public static Message newMessageFromProxyUri(Message message)
			throws URISyntaxException {
		if (message == null) {
			throw new IllegalArgumentException("message == null");
		}
		
		// get the code from the message
		int code = message.getCode();
		
		// create the new message
		Message result = null;
		try {
			result = CodeRegistry.getMessageClass(code).newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result.setCode(code);
		
		// set type
		MessageType messageType = message.getType();
		result.setType(messageType);
		
		// copy payload
		byte[] payload = message.getPayload();
		result.setPayload(payload);
		
		// set a new MID
		int mid = TransactionLayer.nextMessageID();
		result.setMID(mid);
		
		// set the address from the proxy-uri option
		String proxyUriString = getProxyUri(message);
		URI serverUri = new URI(proxyUriString);
		result.setURI(serverUri);
		
		// copy every option but the proxy-uri
		for (Option option : message.getOptions()) {
			if ((option.getOptionNumber() != OptionNumberRegistry.PROXY_URI)
					&& (option.getOptionNumber() != OptionNumberRegistry.URI_PATH)) {
				result.setOption(option);
			}
		}
		
		// insert the forwarded message into the map
		forwardedMessageMap.put(result.getPeerAddress().toString(), message);
		
		return result;
	}
	
	public static Message newMessageFromForwardedMessage(Message forwardedMessage) {
		if (forwardedMessage == null) {
			throw new IllegalArgumentException("message == null");
		}
		
		Message result = null;
		
		// get the mid to retrieve the original message
		//		int mid = forwardedMessage.getMID();
		String sourceAddress = forwardedMessage.getPeerAddress().toString();
		Message originalMessage = forwardedMessageMap.get(sourceAddress);
		
		if (originalMessage != null) {
			forwardedMessageMap.remove(sourceAddress);
			
			// get the code from the forwarded message
			int code = forwardedMessage.getCode();
			
			// create the new message
			try {
				result = CodeRegistry.getMessageClass(code).newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			result.setCode(code);
			
			// set the type
			MessageType messageType = forwardedMessage.getType();
			result.setType(messageType);
			
			// copy payload
			byte[] payload = forwardedMessage.getPayload();
			result.setPayload(payload);
			
			// copy MID
			int originalMid = originalMessage.getMID();
			result.setMID(originalMid);
			
			// set the address from the proxy-uri option
			EndpointAddress peerAddress = originalMessage.getPeerAddress();
			result.setPeerAddress(peerAddress);
			
			// copy every option
			for (Option option : forwardedMessage.getOptions()) {
				result.setOption(option);
			}
		}
		
		return result;
	}
	
	public static Message changeDestinationFromProxyUri(Message message)
			throws URISyntaxException {
		if (message == null) {
			throw new IllegalArgumentException("message == null");
		}
		
		// get the proxy-uri option number
		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		
		// get the proxy-uri string
		String proxyUriString = getProxyUri(message);
		
		// TODO test the string against multiple regexp
		
		// create the URI
		URI serverUri = new URI(proxyUriString);
		
		// modify the request to translate the proxy-uri option in: uri-host, uri-port, uri-path, uri-query
		synchronized (message) { // TODO check sync
			// delete proxy-uri option
			message.removeOption(proxyUriOptNumber);
			
			// set the new URI
			message.setURI(serverUri);
		}
		
		LOG.fine("MESSAGE TRANSLATED");
		//		request.prettyPrint();
		
		return message;
	}
	
}
