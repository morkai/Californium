/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.examples;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Communicator;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.util.Log;

/**
 * This class implements a simple CoAP client for testing purposes. Usage:
 * <p>
 * {@code java -jar SampleClient.jar [-l] METHOD URI [PAYLOAD]}
 * <ul>
 * <li>METHOD: {GET, POST, PUT, DELETE, DISCOVER, OBSERVE}
 * <li>URI: The URI to the remote endpoint or resource}
 * <li>PAYLOAD: The data to send with the request}
 * </ul>
 * Options:
 * <ul>
 * <li>-l: Loop for multiple responses}
 * </ul>
 * Examples:
 * <ul>
 * <li>{@code SampleClient DISCOVER coap://localhost}
 * <li>{@code SampleClient POST coap://someServer.org:5683 my data}
 * </ul>
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class FrequencyClient {
	
	private static final String URI = "coap://localhost/helloWorld";
	
	/*
	 * Main method of this client.
	 */
	public static void main(String[] args) {
		
		Log.setLevel(Level.ALL);
		Log.init();
		
		// with this directive the client have the RateControlLayer disabled
		Communicator.setRequestPerSecond(0);
		
		// create the maps for logging purpose
		final ConcurrentHashMap<Integer, Double> delayMap = new ConcurrentHashMap<Integer, Double>();
		final Vector<Integer> requestsVector = new Vector<Integer>();
		
		// creation of the timer that will measure the distribution of the responses
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			
			int previousSize = 0;
			
			@Override
			public void run() {
				if (this.previousSize != delayMap.size()) {
					requestsVector.add(delayMap.size() - this.previousSize);
					this.previousSize = delayMap.size();
				}
			}
		}, 0, 1000);
		
		// parameters
		int secondsOfTest = 5;
		int requestsPerSecond = 50;
		boolean sendBurst = true;
		
		// the external cycle represents the duration of the test without any constraint
		for (int i = 0; i < secondsOfTest; i++) {
			
			// the main thread sends bursts of request with this sleep
			if (sendBurst) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			for (int j = 0; j < requestsPerSecond; j++) {
				
				// the main thread has a normal distribution of the requests with this sleep
				if (!sendBurst) {
					try {
						Thread.sleep(1000 / requestsPerSecond);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
				// create request according to specified method
				Request request = new Request(CodeRegistry.METHOD_GET, false) {
					@Override
					protected void handleResponse(Response response) {
						//response.prettyPrint();
						
						delayMap.put(response.getMID(), response.getRTT());
						
						System.out.println("Time elapsed (ms): "
								+ response.getRTT());
					}
				};
				
				request.setURI(URI);
				//				request.setPayload(payload);
				request.setToken( TokenManager.getInstance().acquireToken() );
				
				
				// request.prettyPrint();
				
				// execute request
				try {
					request.execute();
					
				} catch (UnknownHostException e) {
					System.err.println("Unknown host: " + e.getMessage());
					System.exit(-1);
				} catch (IOException e) {
					System.err.println("Failed to execute request: " + e.getMessage());
					System.exit(-1);
				}
				
				// finish
				System.out.println();
			}
		}
		
		// wait the end of the request
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// end the timer for stats
		timer.cancel();
		
		// remove the last measure in order not to have wrong numbers
		requestsVector.remove(requestsVector.size() - 1);
		
		// DURATION
		System.out.println("Duration expected: " + secondsOfTest
				+ "s, duration actual: " + requestsVector.size() + "s");
		
		//
		double count = 0;
		for (Double rtt : delayMap.values()) {
			count += rtt;
		}
		
		double avg = count / delayMap.size();
		
		System.out.println("AVG expected: "
				+ (((double) 1000) / requestsPerSecond)
				+ "ms, AVG actual: "
				+ avg + "ms");
		
		count = 0;
		for (Integer requests : requestsVector) {
			count += requests;
		}
		
		System.out.println("Requests per second expected: " + requestsPerSecond
				+ ", AVG requests per second actual: "
				+ (count / requestsVector.size()));
		
	}
	
	/*
	 * Outputs user guide of this program.
	 */
	public static void printInfo() {
		System.out.println("Californium (Cf) Example Client");
		System.out.println("(c) 2012, Institute for Pervasive Computing, ETH Zurich");
		System.out.println();
		System.out.println("Usage: " + FrequencyClient.class.getSimpleName() + " [-l] METHOD URI [PAYLOAD]");
		System.out.println("  METHOD  : {GET, POST, PUT, DELETE, DISCOVER, OBSERVE}");
		System.out.println("  URI     : The CoAP URI of the remote endpoint or resource");
		System.out.println("  PAYLOAD : The data to send with the request");
		System.out.println("Options:");
		System.out.println("  -l      : Loop for multiple responses");
		System.out.println("           (automatic for OBSERVE and separate responses)");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  ExampleClient DISCOVER coap://localhost");
		System.out.println("  ExampleClient POST coap://vs0.inf.ethz.ch:5683/storage my data");
	}
	
	/*
	 * Instantiates a new request based on a string describing a method.
	 * 
	 * @return A new request object, or null if method not recognized
	 */
	private static Request newRequest(String method) {
		if (method.equals("GET")) {
			return new GETRequest();
		} else if (method.equals("POST")) {
			return new POSTRequest();
		} else if (method.equals("PUT")) {
			return new PUTRequest();
		} else if (method.equals("DELETE")) {
			return new DELETERequest();
		} else if (method.equals("DISCOVER")) {
			return new GETRequest();
		} else if (method.equals("OBSERVE")) {
			return new GETRequest();
		} else {
			return null;
		}
	}
	
}
