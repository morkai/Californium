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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.util.Log;

public class FrequencyClient {
	
	// resources
	private String serverUri = "coap://localhost:5684/timeResource";
	private String proxyUri = "coap://localhost/proxy";
	private Option proxyUriOption = new Option(
			"coap://localhost:5684/timeResource",
			OptionNumberRegistry.PROXY_URI);
	
	// parameters
	private int secondsOfTest = 20;
	private int requestsPerSecond = 100;
	private boolean sendBurst = false;
	private boolean testProxy = false;
	
	// maps for logging purpose
	private final ConcurrentHashMap<Integer, Double> delayMap = new ConcurrentHashMap<Integer, Double>();
	private final List<Integer> responseSet = Collections
			.synchronizedList(new LinkedList<Integer>());
	
	// executors
	private final ScheduledExecutorService requestScheduler = Executors
			.newScheduledThreadPool(this.secondsOfTest);
	private final ScheduledExecutorService measureScheduler = Executors
			.newSingleThreadScheduledExecutor();
	
	public FrequencyClient() {
	}
	
	/**
	 * 
	 */
	public void start() {
		// start the measurement thread
		this.measureScheduler.scheduleWithFixedDelay(new MesureRunnable(), 0, 1,
				TimeUnit.SECONDS);
		
		// start the execution thread for the requests
		long delay = this.sendBurst ? 1000000 : 1000000 / this.requestsPerSecond;
		this.requestScheduler.scheduleWithFixedDelay(new RequestRunnable(), 0,
				delay, TimeUnit.NANOSECONDS);
		
		// wait the termination of the requests and the measurements
		try {
			this.requestScheduler.awaitTermination(this.secondsOfTest * 2,
					TimeUnit.SECONDS);
			this.measureScheduler.awaitTermination(this.secondsOfTest * 2,
					TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// finish
		System.out.println();
	}
	
	public boolean isTerminated() {
		return this.requestScheduler.isTerminated()
				&& this.measureScheduler.isTerminated();
	}
	
	/**
	 * 
	 */
	public void printStats() {
		// remove the first measure in order not to have wrong numbers
		this.responseSet.remove(new Integer(0));
		
		// DURATION
		System.out.println("Duration expected: " + this.secondsOfTest
				+ "s, actual duration: " + this.responseSet.size() + "s");
		
		// RESPONSE TIME
		System.out.println("*** RESPONSE TIME ***");
		double count = 0;
		for (Double rtt : this.delayMap.values()) {
			count += rtt;
		}
		double avg = count / this.delayMap.size();
		System.out.println("AVG expected: "
				+ (((double) 1000) / this.requestsPerSecond)
				+ "ms, AVG actual: "
				+ avg + "ms");
		Double max = Collections.max(this.delayMap.values());
		Double min = Collections.min(this.delayMap.values());
		System.out.println("Max: " + max + "ms, min: " + min + "ms");
		
		// NUMBER OF RESPONSES
		System.out.println("*** NUMBER OF RESPONSES ***");
		count = 0;
		for (Integer requests : this.responseSet) {
			count += requests;
		}
		System.out.println("Tot requests: "
				+ (this.requestsPerSecond * this.secondsOfTest)
				+ ", tot responses: "
				+ count);
		System.out.println("Requests per second sent: " + this.requestsPerSecond
				+ ", AVG response per second received: "
				+ (count / this.responseSet.size()));
		int max1 = Collections.max(this.responseSet);
		int min1 = Collections.min(this.responseSet);
		System.out.println("Max: " + max1 + ", min: " + min1);
	}
	
	/**
	 * @param uRI the uRI to set
	 */
	public void setURI(String uRI) {
		this.serverUri = uRI;
	}
	
	/**
	 * @param uRI_PROXY the uRI_PROXY to set
	 */
	public void setURI_PROXY(String uRI_PROXY) {
		this.proxyUri = uRI_PROXY;
	}
	
	/**
	 * @param proxyUri the proxyUri to set
	 */
	public void setProxyUri(Option proxyUri) {
		this.proxyUriOption = proxyUri;
	}
	
	/**
	 * @param secondsOfTest the secondsOfTest to set
	 */
	public void setSecondsOfTest(int secondsOfTest) {
		this.secondsOfTest = secondsOfTest;
	}
	
	/**
	 * @param requestsPerSecond the requestsPerSecond to set
	 */
	public void setRequestsPerSecond(int requestsPerSecond) {
		this.requestsPerSecond = requestsPerSecond;
	}
	
	/**
	 * @param sendBurst the sendBurst to set
	 */
	public void setSendBurst(boolean sendBurst) {
		this.sendBurst = sendBurst;
	}
	
	/**
	 * @param testProxy the testProxy to set
	 */
	public void setTestProxy(boolean testProxy) {
		this.testProxy = testProxy;
	}
	
	class RequestRunnable implements Runnable {
		// the dimension of the delayMap on the previous measure
		private volatile Integer iterations = (FrequencyClient.this.sendBurst ? FrequencyClient.this.secondsOfTest
				: FrequencyClient.this.secondsOfTest
				* FrequencyClient.this.requestsPerSecond);
		
		@Override
		public void run() {
			boolean proceed = false;
			
			//			System.out.println(this.iterations);
			synchronized (this.iterations) {
				if (this.iterations > 0) {
					this.iterations--;
					proceed = true;
				}
			}
			
			if (proceed) {
				if (FrequencyClient.this.sendBurst) {
					for (int j = 0; j < FrequencyClient.this.requestsPerSecond; j++) {
						getResponse();
					}
				} else {
					getResponse();
				}
			}
			
			// terminate the executor
			if (this.iterations == 0) {
				FrequencyClient.this.requestScheduler.shutdownNow();
			}
		}
		
		/**
		 */
		private void getResponse() {
			// create request according to specified method
			//			Request request = new Request(CodeRegistry.METHOD_GET, false) {
			Request request = new GETRequest() {
				@Override
				protected void handleResponse(Response response) {
					//response.prettyPrint();
					
					FrequencyClient.this.delayMap.put(response.getMID(),
							response.getRTT());
					
					//						System.out.println("Time elapsed (ms): "
					//								+ response.getRTT());
				}
			};
			
			if (FrequencyClient.this.testProxy) {
				request.setURI(FrequencyClient.this.proxyUri);
				request.setOption(FrequencyClient.this.proxyUriOption);
			} else {
				request.setURI(FrequencyClient.this.serverUri);
			}
			request.setToken(TokenManager.getInstance().acquireToken());
			
			// execute request
			try {
				request.execute();
			} catch (UnknownHostException e) {
				System.err.println("Unknown host: " + e.getMessage());
				//					System.exit(-1);
			} catch (IOException e) {
				System.err.println("Failed to execute request: "
						+ e.getMessage());
				//					System.exit(-1);
			}
		}
	}
	
	class MesureRunnable implements Runnable {
		// the dimension of the delayMap on the previous measure
		int previousMapSize = 0;
		int totRequests = FrequencyClient.this.secondsOfTest * FrequencyClient.this.requestsPerSecond;
		
		@Override
		public void run() {
			// run until there are requests without a response
			if (this.totRequests > 10) {
				// add the number of the request performed in the last time slot
				int requestCompleted = FrequencyClient.this.delayMap.size()
						- this.previousMapSize;
				// log only if there are responses
				this.totRequests -= requestCompleted;
				FrequencyClient.this.responseSet.add(requestCompleted);
				
				System.out.println("Second " + FrequencyClient.this.responseSet.size()
						+ ", requests completed: " + requestCompleted
						+ ", remaining " + this.totRequests + " requests");
				
				// update the previous value
				this.previousMapSize = FrequencyClient.this.delayMap.size();
			} else {
				// terminate the execution if there are no more responses to wait for
				FrequencyClient.this.measureScheduler.shutdownNow();
			}
		}
	}
	
	/*
	 * Main method of this client.
	 */
	public static void main(String[] args) {
		Log.setLevel(Level.SEVERE);
		Log.init();
		
		FrequencyClient frequencyClient = new FrequencyClient();
		
		frequencyClient.start();
		
		while (!frequencyClient.isTerminated()) {
			;
		}
		
		frequencyClient.printStats();
		
		System.exit(0);
	}
}
