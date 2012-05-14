/**
 * 
 */
package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;

/**
 * @author Francesco Corazza
 *
 */
public class CoapProxyTest {
	//	private static int SERVER_PORT = 5684;
	
	//	private static Process serverProcess;
	//	private static Process proxyProcess;
	
	private static final String proxyLocation = "coap://localhost";
	private static final String serverLocation = "coap://localhost:5684";
	
	/**
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		//		try {
		//			serverProcess = Runtime.getRuntime().exec(
		//					"java -jar run/ExampleServer.jar " + SERVER_PORT);
		//			proxyProcess = Runtime.getRuntime().exec(
		//					"java -jar run/ExampleProxy.jar");
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		
		//		try {
		//			serverProcess = new ProcessBuilder(
		//					"java -jar ../run/ExampleServer.jar",
		//					Integer.toString(SERVER_PORT)).start();
		//			proxyProcess = new ProcessBuilder(
		//					"java -jar ../run/ExampleProxy.jar")
		//			.start();
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		
	}
	
	/**
	 */
	@AfterClass
	public static void tearDownAfterClass() {
		//		proxyProcess.destroy();
		//		serverProcess.destroy();
	}
	
	@Before
	public void setUp() {
		
	}
	
	@After
	public void tearDown() {
		// TODO
	}
	
	/**
	 * @param resource
	 * @return
	 */
	private Response executeRequest(Request request, String resource, boolean enableProxying) {
		String proxyResource = "proxy";
		
		if (enableProxying) {
			Option proxyUriOption = new Option(serverLocation + "/" + resource,
					OptionNumberRegistry.PROXY_URI);
			request.setOption(proxyUriOption);
		} else {
			proxyResource = resource;
		}
		
		Option uriPathOption = new Option(proxyResource,
				OptionNumberRegistry.URI_PATH);
		request.setOption(uriPathOption);
		request.setURI(proxyLocation);
		request.setToken(TokenManager.getInstance().acquireToken());
		
		// enable response queue for synchronous I/O
		request.enableResponseQueue(true);
		
		// execute the request
		try {
			request.execute();
		} catch (IOException e) {
			System.err.println("Failed to execute request: " + e.getMessage());
		}
		
		// receive response
		Response response = null;
		try {
			response = request.receiveResponse();
		} catch (InterruptedException e) {
			System.err.println("Receiving of response interrupted: "
					+ e.getMessage());
		}
		return response;
	}
	
	@Test
	public void localProxyResourceTest() {
		String getResource = "proxy";
		
		Request getRequest = new GETRequest();
		Response response = executeRequest(getRequest, getResource, false);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
	}
	
	@Test
	public void localWrongProxyResourceTest() {
		String getResource = "inexistent";
		
		Request getRequest = new GETRequest();
		Response response = executeRequest(getRequest, getResource, false);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_NOT_FOUND);
	}
	
	@Test
	public void wrongProxyResourceTest() {
		String getResource = "inexistent";
		
		Request getRequest = new GETRequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_NOT_FOUND);
	}
	
	@Test
	public void helloWorldGetTest() {
		String getResource = "helloWorld";
		
		Request getRequest = new GETRequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
	}
	
	@Test
	public void longPathGetTest() {
		String getResource = "seg1/seg2/seg3";
		
		Request getRequest = new GETRequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
	}
	
	@Test
	public void timeResourceGetTest() {
		String getResource = "timeResource";
		
		Request getRequest = new GETRequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
	}
	
	@Test
	public void timeResourcePostFailTest() {
		String getResource = "timeResource";
		
		Request getRequest = new POSTRequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}
	
	@Test
	public void timeResourcePutFailTest() {
		String getResource = "timeResource";
		
		Request getRequest = new PUTRequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}
	
	@Test
	public void timeResourceDeleteFailTest() {
		String getResource = "timeResource";
		
		Request getRequest = new DELETERequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}
	
	@Test
	public void toUpperPutTest() {
		String postResource = "toUpper";
		String requestPayload = "aaa";
		
		Request postRequest = new POSTRequest();
		postRequest.setPayload(requestPayload);
		postRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		
		Response response = executeRequest(postRequest, postResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
		
		String responsePayload = response.getPayloadString();
		assertTrue(responsePayload.equals(requestPayload.toUpperCase()));
	}
	
	@Test
	public void storagePutGetTest() {
		String putResource = "storage";
		String requestPayload = "aaa";
		
		Request putRequest = new PUTRequest();
		putRequest.setPayload(requestPayload);
		putRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		
		Response putResponse = executeRequest(putRequest, putResource, true);
		assertNotNull(putResponse);
		assertTrue(putResponse.getCode() == CodeRegistry.RESP_CHANGED);
		
		Request getRequest = new GETRequest();
		Response getResponse = executeRequest(getRequest, putResource, true);
		
		assertNotNull(getResponse);
		assertTrue(getResponse.getCode() == CodeRegistry.RESP_CONTENT);
		
		String responsePayload = getResponse.getPayloadString();
		assertTrue(responsePayload.equals(requestPayload));
	}
	
	@Test
	public void storagePostGetTest() {
		String postResource = "storage";
		String requestPayload = "subResource";
		
		Request postRequest = new POSTRequest();
		postRequest.setPayload(requestPayload);
		postRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		
		Response postResponse = executeRequest(postRequest, postResource, true);
		assertNotNull(postResponse);
		assertTrue(postResponse.getCode() == CodeRegistry.RESP_CREATED);
		
		Request getRequest = new GETRequest();
		Response getResponse = executeRequest(getRequest, postResource + "/"
				+ requestPayload, true);
		
		assertNotNull(getResponse);
		assertTrue(getResponse.getCode() == CodeRegistry.RESP_CONTENT);
		
		String responsePayload = getResponse.getPayloadString();
		assertTrue(responsePayload.equals(requestPayload));
	}
	
	@Test
	public void storagePostDeleteTest() {
		String postResource = "storage";
		String requestPayload = "subResource2";
		
		Request postRequest = new POSTRequest();
		postRequest.setPayload(requestPayload);
		postRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		
		Response postResponse = executeRequest(postRequest, postResource, true);
		assertNotNull(postResponse);
		assertTrue(postResponse.getCode() == CodeRegistry.RESP_CREATED);
		
		Request deleteRequest = new DELETERequest();
		Response deleteResponse = executeRequest(deleteRequest, postResource + "/"
				+ requestPayload, true);
		
		assertNotNull(deleteResponse);
		assertTrue(deleteResponse.getCode() == CodeRegistry.RESP_DELETED);
	}
	
	@Test
	public void largeGetTest() {
		String getResource = "large";
		
		Request getRequest = new GETRequest();
		Response response = executeRequest(getRequest, getResource, true);
		
		assertNotNull(response);
		assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
	}
	
	@Test
	public void timeResourceGetAsyncTest() {
		String getResource = "timeResource";
		Option proxyUriOption = new Option(
				serverLocation + getResource,
				OptionNumberRegistry.PROXY_URI);
		
		Request request = new GETRequest() {
			@Override
			protected void handleResponse(Response response) {
				assertNotNull(response);
				
				//						System.out.println("Time elapsed (ms): "
				//								+ response.getRTT());
			}
		};
		
		request.setURI(proxyLocation);
		request.setOption(proxyUriOption);
		request.setToken(TokenManager.getInstance().acquireToken());
		
		// execute request
		try {
			request.execute();
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Failed to execute request: " + e.getMessage());
		}
	}
	
}
