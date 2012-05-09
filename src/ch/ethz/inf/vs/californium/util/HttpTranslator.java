/**
 * 
 */
package ch.ethz.inf.vs.californium.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ParseException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.EntityUtils;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

/**
 * @author Francesco Corazza
 *
 */
public class HttpTranslator {
	
	private static final String PROPERTIES_FILENAME = "Proxy.properties";
	
	private static final Properties properties = new Properties(
			PROPERTIES_FILENAME);
	
	protected static final Logger LOG = Logger
			.getLogger(HttpTranslator.class.getName());
	
	public static Request createCoapRequest(HttpRequest httpRequest)
			throws HttpException {
		if (httpRequest == null) {
			throw new IllegalArgumentException("httpRequest == null");
		}
		
		// get the method
		String method = httpRequest.getRequestLine().getMethod().toLowerCase();
		String coapMethod = properties.getProperty("http.request.method."
				+ method);
		
		if (coapMethod.contains("error")) {
			throw new MethodNotSupportedException(method
					+ " method not supported");
		}
		
		// create the new request
		Request coapRequest = null;
		try {
			Message message = CodeRegistry.getMessageClass(
					Integer.parseInt(coapMethod)).newInstance();
			
			// safe cast
			if (message instanceof Request) {
				coapRequest = (Request) message;
			} else {
				LOG.severe("Failed to convert request number " + coapMethod);
				throw new HttpException(coapMethod + " not recognized");
			}
		} catch (NumberFormatException e) {
			LOG.severe("Failed to convert request number " + coapMethod + ": "
					+ e.getMessage());
			throw new HttpException("Error in creating the request: "
					+ coapMethod, e);
		} catch (InstantiationException e) {
			LOG.severe("Failed to convert request number " + coapMethod + ": "
					+ e.getMessage());
			throw new HttpException("Error in creating the request: "
					+ coapMethod, e);
		} catch (IllegalAccessException e) {
			LOG.severe("Failed to convert request number " + coapMethod + ": "
					+ e.getMessage());
			throw new HttpException("Error in creating the request: "
					+ coapMethod, e);
		}
		
		// set the uri
		String uriString = httpRequest.getRequestLine().getUri();
		// check if the query string is present
		// if there is no queries, the request is intended for the local http server
		if (uriString.contains("?")) {
			// get the url in the request
			uriString = uriString.split("\\?")[1];
			
			// add the scheme if not present
			if (!uriString.contains("coap")) {
				uriString = "coap://" + uriString;
			}
		}
		
		URI uri = null;
		try {
			uri = new URI(uriString);
			coapRequest.setURI(uri);
			
			// check for the correctness of the uri
			if (!coapRequest.getPeerAddress().isInitialized()) {
				throw new URISyntaxException(uriString, "URI malformed");
			}
		} catch (URISyntaxException e) {
			LOG.severe("URI malformed: " + e.getMessage());
			throw new ParseException("URI malformed");
		}
		
		// set the headers
		HeaderIterator headerIterator = httpRequest.headerIterator();
		while (headerIterator.hasNext()) {
			Header header = headerIterator.nextHeader();
			String optionCodeString = properties
					.getProperty("http.request.header."
							+ header.getName().toLowerCase());
			
			// ignore the header if not found in the properties
			if (optionCodeString != null) {
				// create the option for the current header
				int optionCode = Integer.parseInt(optionCodeString);
				Option option = new Option(optionCode);
				// get the value of the current header
				String headerValue = (header instanceof BufferedHeader) ? ((BufferedHeader) header)
						.getBuffer().toString().split(" ")[1]
								: header.getValue();
						option.setStringValue(headerValue); // TODO check for the non string values
						coapRequest.setOption(option);
			}
		}
		
		// get the http entity if any in the http request
		if (httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntity httpEntity = ((HttpEntityEnclosingRequest) httpRequest)
					.getEntity();
			
			if (httpEntity != null) {
				// get the content type of the entity
				String contentTypeString = EntityUtils.getContentMimeType(httpEntity);
				
				// set the content type in the request if it is recognized
				int contentType = MediaTypeRegistry.parse(contentTypeString);
				if (contentType != MediaTypeRegistry.UNDEFINED) {
					// TODO add additional conversions
					coapRequest.setContentType(contentType);
					
					try {
						// copy the http entity in the payload of the coap request
						byte[] payload = EntityUtils.toByteArray(httpEntity);
						coapRequest.setPayload(payload);
						
						// ensure all content has been consumed, so that the underlying connection could be re-used
						EntityUtils.consume(httpEntity);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					// the lenth is not known in advance
					// TODO
					System.out.println("LENGHT NOT KNOWN"); // FIXME
				}
			}
		}
		
		// DEBUG
		coapRequest.prettyPrint();
		
		return coapRequest;
	}
	
	public static void fillHttpResponse(HttpRequest httpRequest,
			HttpResponse httpResponse, Response coapResponse)
					throws UnsupportedEncodingException {
		if (httpResponse == null) {
			throw new IllegalArgumentException("httpResponse == null");
		}
		if (coapResponse == null) {
			throw new IllegalArgumentException("coapResponse == null");
		}
		
		// DEBUG
		coapResponse.prettyPrint();
		
		// get/set the response code
		int coapCode = coapResponse.getCode();
		String httpCodeString = properties.getProperty("coap.response.code."
				+ coapCode);
		int httpCode = Integer.parseInt(httpCodeString);
		httpResponse.setStatusLine(HttpVersion.HTTP_1_1,
				httpCode);
		
		// add the entity to the response if present a payload and if the http method was not HEAD
		String method = httpRequest.getRequestLine().getMethod().toLowerCase();
		HttpEntity httpEntity = null;
		if ((coapResponse.getPayload().length != 0)
				&& !method.equalsIgnoreCase("HEAD")) {
			// get/set the payload as entity
			byte[] payload = coapResponse.getPayload();
			httpEntity = new ByteArrayEntity(payload);
			httpResponse.setEntity(httpEntity);
			
			// get/set the content-type
			int coapContentType = coapResponse.getContentType();
			String contentType = MediaTypeRegistry.toString(coapContentType);
			Header contentTypeHeader = new BasicHeader("content-type",
					contentType);
			httpResponse.setHeader(contentTypeHeader);
		} else {
			httpEntity = new StringEntity(""); // FIXME
			httpResponse.setEntity(httpEntity);
		}
		// set a null entity to unset it
		httpResponse.setEntity(httpEntity);
		
		//		BasicHttpEntityEnclosingRequest
		//		BasicHttpEntity myEntity = new BasicHttpEntity();
		//		myEntity.setContent(someInputStream);
		//		myEntity.setContentLength(340); // sets the length to 340
		//		// alternatively construct with an encoding and a mime type
		//		HttpEntity myEntity3 = new StringEntity(sb.toString(), "text/html",
		//				"UTF-8");
		//		StringEntity myEntity = new StringEntity("important message", "UTF-8");
		//		HttpEntity entity = response.getEntity();
		//		if (entity != null) {
		//			InputStream instream;
		//			try {
		//				instream = entity.getContent();
		//			} catch (IllegalStateException e1) {
		//				// TODO Auto-generated catch block
		//				e1.printStackTrace();
		//			} catch (IOException e1) {
		//				// TODO Auto-generated catch block
		//				e1.printStackTrace();
		//			}
		//			try {
		//				// do something useful
		//			} finally {
		//				try {
		//					instream.close();
		//				} catch (IOException e) {
		//					// TODO Auto-generated catch block
		//					e.printStackTrace();
		//				}
		//			}
		//		}
		//			response.setStatusLine(targetResponse.getStatusLine());
		//			response.setHeaders(targetResponse.getAllHeaders());
		//			response.setEntity(targetResponse.getEntity());
		
	}
}
