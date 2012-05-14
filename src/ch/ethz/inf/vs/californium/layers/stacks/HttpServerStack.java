/**
 * 
 */
package ch.ethz.inf.vs.californium.layers.stacks;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.HttpTranslator;


/**
 * @author Francesco Corazza
 *
 */
public class HttpServerStack extends AbstractStack {
	private int port;
	private ExecutorService threadPool;
	
	public HttpServerStack(int port, ExecutorService threadPool) {
		super(true);
		// initialize layers and the stack
		this.port = port;
		
		this.threadPool = threadPool;

		Thread thread = null;
		try {
			thread = new RequestListenerThread(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		thread.setDaemon(false);
		thread.start();
	}
	
	@Override
	public int getPort() {
		return this.port;
	}
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		super.doSendMessage(msg);
		
		System.out.println("HttpServerStack - SENT MESSAGE ?");
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {
		
		super.doReceiveMessage(msg);
		
		System.out
		.println("HttpServerStack - RECEIVED MESSAGE AND SENT TO COMMUNICATOR");
	}
	
	static class ProxyHandler implements HttpRequestHandler {
		
		public ProxyHandler() {
			super();
		}
		
		@Override
		public void handle(final HttpRequest httpRequest,
				final HttpResponse httpResponse, final HttpContext context)
						throws HttpException, IOException {
			
			// DEBUG
			System.out.println(">> Request: " + httpRequest);
			
			// get the coap request
			Request coapRequest = HttpTranslator.createCoapRequest(httpRequest);
			
			// throw an exception if it not possible to create a new coap request
			if (coapRequest == null) {
				throw new HttpException("Cannot create a coap request.");
			}
			
			// enable response queue for synchronous I/O
			coapRequest.enableResponseQueue(true);
			
			// execute the request
			try {
				coapRequest.execute();
			} catch (IOException e) {
				LOG.severe("Failed to execute request: "
						+ e.getMessage());
				throw new HttpException("Failed to execute request", e);
			}
			
			// receive response
			Response coapResponse = null;
			try {
				coapResponse = coapRequest.receiveResponse();
				
			} catch (InterruptedException e) {
				LOG.severe("Receiving of response interrupted: "
						+ e.getMessage());
				throw new HttpException("Receiving of response interrupted", e);
			}
			
			if (coapResponse != null) {
				// translate the received response to the http response
				HttpTranslator.fillHttpResponse(httpRequest, httpResponse,
						coapResponse);
			} else {
				LOG.severe("No response received.");
				throw new NoHttpResponseException("No response received."); // TODO check?
			}
			
			// DEBUG
			System.out.println("<< Response: " + httpResponse);
		}
	}
	
	class RequestListenerThread extends Thread {
		
		private static final String SERVER_NAME = "Californium Proxy";
		private final ServerSocket serversocket;
		private final HttpParams params;
		private final HttpService httpService;
		
		public RequestListenerThread(int port)
				throws IOException {
			super("HTTP RequestListener");
			this.serversocket = new ServerSocket(port);
			this.params = new SyncBasicHttpParams();
			this.params
			.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
			.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
					8 * 1024)
					.setBooleanParameter(
							CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
							.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
							.setParameter(CoreProtocolPNames.ORIGIN_SERVER,
									SERVER_NAME);
			
			// Set up the HTTP protocol processor
			HttpProcessor httpproc = new ImmutableHttpProcessor(
					new HttpResponseInterceptor[] { new ResponseDate(),
							new ResponseServer(), new ResponseContent(),
							new ResponseConnControl() });
			
			// Set up request handlers
			HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
			reqistry.register("*", new ProxyHandler());
			
			// Set up the HTTP service
			this.httpService = new HttpService(httpproc,
					new DefaultConnectionReuseStrategy(),
					new DefaultHttpResponseFactory(), reqistry, this.params);
		}
		
		@Override
		public void run() {
			System.out.println("Listening on port "
					+ this.serversocket.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					// Set up incoming HTTP connection
					Socket insocket = this.serversocket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					System.out.println("Incoming connection from "
							+ insocket.getInetAddress());
					conn.bind(insocket, this.params);
					
					// Start worker thread
					HttpServerStack.this.threadPool
							.submit(new HTTPServiceRunnable(this.httpService,
									conn));
					
					//					Thread t = new HTTPServiceThread(this.httpService, conn);
					//					t.setDaemon(true);
					//					t.start();
				} catch (InterruptedIOException ex) {
					break;
				} catch (IOException e) {
					System.err
					.println("I/O error initialising connection thread: "
							+ e.getMessage());
					break;
				}
			}
		}
	}
	
	class HTTPServiceRunnable implements Runnable {
		
		private final HttpService httpservice;
		private final HttpServerConnection conn;
		
		public HTTPServiceRunnable(final HttpService httpservice,
				final HttpServerConnection conn) {
			this.httpservice = httpservice;
			this.conn = conn;
		}
		
		@Override
		public void run() {
			System.out.println("New connection thread");
			HttpContext context = new BasicHttpContext(null);
			
			try {
				while (!Thread.interrupted() && this.conn.isOpen()) {
					this.httpservice.handleRequest(this.conn, context);
				}
			} catch (ConnectionClosedException ex) {
				System.err.println("Client closed connection");
			} catch (IOException ex) {
				System.err.println("I/O error: " + ex.getMessage());
			} catch (HttpException ex) {
				System.err.println("Unrecoverable HTTP protocol violation: "
						+ ex.getMessage());
			} finally {
				try {
					this.conn.shutdown();
				} catch (IOException ignore) {
				}
			}
		}
		
	}
	
	@Override
	protected void createStack() throws SocketException {
		// TODO Auto-generated method stub
		
	}
	
}
