package com.ociweb;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Test;

import com.ociweb.gl.api.GreenRuntime;

/**
 * Unit test for simple App.
 */
public class AppTest { 

		private final long timeoutMS = 260_000;

		
		@Test
	    public void testBackingServerSequential() {
	    	GreenRuntime.run(new TestServer(false, 8082, true));
	    	waitForServer("http://127.0.0.1:8082/");
	    	String route = "/testPage";
	    	
		    GreenRuntime.testUntilShutdownRequested(new TestClientSequential(20000,8082,route), timeoutMS);	    	
	    }
	    
	    @Test
	    public void testBackingServerConcurrent() {
	    	GreenRuntime.run(new TestServer(false, 8082, true));
	    	waitForServer("http://127.0.0.1:8082/");
	    	String route = "/testPage";
	    	
	    	//NOTE: server is picking the same route for every call??
	    	
	    	GreenRuntime.testConcurrentUntilShutdownRequested(
	    			new TestClientParallel(20000,8082, route, true), timeoutMS);
	    	
	    	
        }
		
		
		@Test
	    public void textProxyServer() {
			
			//startup backing server
			GreenRuntime.run(new TestServer(false, 8082, false));
	    	waitForServer("http://127.0.0.1:8082/");
	    	
	    	//startup proxy server in front of backing server
	    	GreenRuntime.run(new GreenProxy("127.0.0.1",8082)); // 8786
	    	waitForServer("http://127.0.0.1:8786/");
	    	
	    	String route = "/testPage";
	    	//GreenRuntime.testConcurrentUntilShutdownRequested(new TestClientParallel(12000,8786), timeoutMS);	 
		    GreenRuntime.testUntilShutdownRequested(new TestClientSequential(20000,8786, route), timeoutMS);	
	    }

		private void waitForServer(String url) {
			try {
			
				waitForServer(new URL(url));
			
			} catch (MalformedURLException e) {
				
				e.printStackTrace();
				fail();
			}
		}
		
		private void waitForServer(URL url) {
			try {
				boolean waiting = true;				
				while (waiting) {
					try {
						URLConnection con = url.openConnection();				
						con.connect();
					} catch (ConnectException ce) {
						continue;
					}
					waiting = false;
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		
		@Test
	    public void testBackingFileServerSequential() {
	    	GreenRuntime.run(new TestFileServer(8083));
	    	waitForServer("http://127.0.0.1:8083/");
	    	String route = "/index.html";
	    	
	    	GreenRuntime.testUntilShutdownRequested(new TestClientSequential(20000, 8083, route), timeoutMS);	    	

	    }
		
}
