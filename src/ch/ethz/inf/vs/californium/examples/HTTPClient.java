/**
 * 
 */
package ch.ethz.inf.vs.californium.examples;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Francesco Corazza
 *
 */
public class HTTPClient {
	
	public static void main(String[] args) {
		if (args.length != 2) {
			
		}
		URL url;
		try {
			url = new URL(args[2]);
			URLConnection connection = url.openConnection(null);
			// connection.

		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
